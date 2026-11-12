use strict;
use warnings;
use Irssi;
use Irssi::Irc;
use JSON::PP;
use IPC::Open2;
use Encode qw(encode decode FB_CROAK);
use Time::HiRes qw(time);

our $VERSION = '1.0';
our %IRSSI = (authors => 'the_bot', name => 'bot_notify', description => 'Send phone-approved drafts through this IRC session', license => 'MIT');
Irssi::settings_add_str('bot_notify', 'bot_notify_helper', "$ENV{HOME}/.local/lib/the-bot/irssi_agent.py");
Irssi::settings_add_str('bot_notify', 'bot_notify_config', "$ENV{HOME}/.config/the-bot/irssi-agent.json");
my ($reader, $writer, $pid, $watch, $buffer);
my $json = JSON::PP->new->utf8;

sub fold {
    my ($s) = @_;
    return lc($s // '');
}
sub respond {
    my ($data) = @_;
    local $SIG{PIPE} = "IGNORE";
    print {$writer} $json->encode($data) . "\n" or Irssi::print("the_bot helper pipe unavailable");
}
sub snapshot {
    my @connections;
    foreach my $server (Irssi::servers()) {
        next unless $server->{connected} && ($server->{chat_type} // '') eq 'IRC';
        push @connections, { network => ($server->{chatnet} || $server->{tag}), nick => $server->{nick},
            channels => [map { $_->{name} } grep { $_->{joined} } $server->channels()] };
    }
    return \@connections;
}
sub chunks {
    my ($text) = @_;
    my (@parts, $part); $part = '';
    foreach my $char (split //, $text) {
        if (length(encode('UTF-8', $part . $char)) > 350) { push @parts, $part; $part = ''; }
        $part .= $char;
    }
    push @parts, $part if length $part;
    return @parts;
}
sub send_job {
    my ($job) = @_;
    my $id = $job->{id};
    my $status = 'FAILED'; my $detail = 'IRC connection, nick or channel unavailable';
    my $sent = 0;
    eval {
        die 'Send deadline passed' unless defined($job->{deadlineEpoch}) && $job->{deadlineEpoch} =~ /\A[0-9]+(?:\.[0-9]+)?\z/ && $job->{deadlineEpoch} > time();
        die 'Invalid channel' unless ($job->{channel} // '') =~ /\A[#&][^\s,:\x00-\x1f\x7f-\x9f]{1,199}\z/;
        die 'Invalid text' if !length($job->{message} // '') || length($job->{message}) > 570 || $job->{message} =~ /[\x00-\x1f\x7f-\x9f]/;
        foreach my $server (Irssi::servers()) {
            next unless $server->{connected} && ($server->{chat_type} // '') eq 'IRC' && fold($server->{chatnet} || $server->{tag}) eq fold($job->{network})
                && fold($server->{nick}) eq fold($job->{expectedNick});
            my $channel = $server->channel_find($job->{channel});
            next unless $channel && $channel->{joined};
            foreach my $part (chunks($job->{message})) {
                $server->send_message($channel->{name}, encode("UTF-8", $part), 0);
                Irssi::signal_emit("message own_public", $server, encode("UTF-8", $part), $channel->{name}); # SEND_TARGET_CHANNEL; literal text, never a /command
                $sent++;
            }
            $status = 'SUBMITTED'; $detail = 'Submitted through Irssi'; last;
        }
    };
    if ($@) { $status = $sent ? 'UNCERTAIN' : 'FAILED'; $detail = 'Could not submit complete reply'; }
    Irssi::print("the_bot reply $id: $status");
    respond({type => 'result', id => $id, status => $status, detail => $detail});
}
sub readable {
    my $data = ''; my $count = sysread($reader, $data, 65536);
    if (!defined($count) || $count == 0) {
        Irssi::input_remove($watch) if defined $watch; undef $watch;
        Irssi::print('the_bot helper stopped; use /bot_notify_restart after fixing its connection/config'); return;
    }
    $buffer .= $data;
    if (length($buffer) > 1024 * 1024) { $buffer = ''; Irssi::print('the_bot helper protocol limit exceeded'); return; }
    while ($buffer =~ s/\A([^\n]*)\n//) {
        my $item = eval { $json->decode($1) }; next unless ref($item) eq 'HASH';
        if (($item->{type} // '') eq 'snapshot') { respond({type => 'snapshot', connections => snapshot()}); }
        elsif (($item->{type} // '') eq 'status') { Irssi::print('the_bot: ' . substr($item->{message} // '', 0, 200)); }
        elsif (($item->{type} // '') eq 'send' && ref($item->{job}) eq 'HASH') { send_job($item->{job}); }
    }
}
sub stop {
    Irssi::input_remove($watch) if defined $watch; undef $watch;
    close $writer if $writer; close $reader if $reader;
    if ($pid) { kill 'TERM', $pid; undef $pid; }
}
sub start {
    stop(); $buffer = '';
    my $helper = Irssi::settings_get_str('bot_notify_helper');
    my $config = Irssi::settings_get_str('bot_notify_config');
    unless (-f $helper && -f $config) { Irssi::print('the_bot: install helper and pair it from the Android Irssi screen first'); return; }
    eval { $pid = open2($reader, $writer, 'python3', '-I', $helper, 'run', '--config', $config); };
    if ($@) { Irssi::print('the_bot: helper could not start'); return; }
    Irssi::pidwait_add($pid);
    my $old = select($writer); $| = 1; select($old);
    $watch = Irssi::input_add(fileno($reader), Irssi::INPUT_READ, \&readable, undef);
    Irssi::print('the_bot: Irssi reply helper started');
}
Irssi::signal_add('pidwait', sub { my ($ended) = @_; undef $pid if defined($pid) && $ended == $pid; });
Irssi::command_bind('bot_notify_restart', \&start);
sub UNLOAD { stop(); }
start();
