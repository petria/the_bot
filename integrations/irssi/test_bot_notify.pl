use strict;
use warnings;
use utf8;
use Test::More;
use Encode qw(decode);
use FindBin;
BEGIN { $INC{'Irssi.pm'}=__FILE__; $INC{'Irssi/Irc.pm'}=__FILE__; }
our (@servers,@sent,@results);
{
    package Irssi;
    sub import {}
    sub INPUT_READ () {1}
    sub settings_add_str {}
    sub settings_get_str { '/nonexistent/the-bot-test-config' }
    sub print {}
    sub servers { @main::servers }
    sub command_bind {}
    sub signal_emit {}
    sub signal_add {}
    sub pidwait_add {}
}
{
    package TestServer;
    sub channels { (bless({name=>'#target',joined=>1},'TestChannel')) }
    sub channel_find { return $_[1] eq '#target' ? {name=>'#target',joined=>1} : undef }
    sub send_message { my ($self,$target,$text,$type)=@_; push @main::sent,[$target,$text,$type] }
    sub command { die 'Command interpreter must never be used' }
}
my $script="$FindBin::Bin/bot_notify.pl";
do $script;
die $@ if $@;
{
    no warnings qw(redefine once);
    *main::respond = sub { push @results,$_[0] };
}
@servers=(bless({connected=>1,chat_type=>'IRC',chatnet=>'IRCnet',tag=>'irc',nick=>'_Pete_'},'TestServer'));
my $job={id=>'job',deadlineEpoch=>time()+60,network=>'IRCNet',channel=>'#target',expectedNick=>'_Pete_',message=>'Alice: /quit'};
main::send_job($job);
is($results[-1]{status},'SUBMITTED','matching session submits');
is_deeply($sent[0],['#target','Alice: /quit',0],'slash is literal message text to original channel');
for my $change ([network=>'other'],[channel=>'#other'],[expectedNick=>'other'],[message=>"bad\n/quit"],[deadlineEpoch=>time()-1]) {
    @sent=();my %invalid=(%$job,$change->[0]=>$change->[1]); main::send_job(\%invalid);
    is(scalar(@sent),0,'invalid destination, nick or control text never sends');
    is($results[-1]{status},'FAILED','failure is returned');
}
@sent=();main::send_job({%$job,message=>'Alice: '.('ä' x 490)});
is($results[-1]{status},'SUBMITTED','Unicode reply submitted');
ok(scalar(@sent)>1,'long UTF-8 reply split safely');
my $combined='';
for my $part (@sent) { ok(length($part->[1])<=350,'IRC chunk within byte budget'); $combined.=decode('UTF-8',$part->[1],Encode::FB_CROAK); }
is($combined,'Alice: '.('ä' x 490),'all Unicode characters preserved');
@sent=();@servers=();main::send_job($job);
is(scalar(@sent),0,'disconnected session cannot send');
is($results[-1]{status},'FAILED','disconnect reported');
done_testing();
