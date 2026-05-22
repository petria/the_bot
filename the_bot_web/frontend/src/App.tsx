import { Alert, AppShell, Avatar, Badge, Box, Burger, Group, Menu, NavLink, Stack, Text, Title, UnstyledButton } from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { Bot, ChevronDown, ListTree, LogOut, RadioTower, Send, Server, Settings, ShieldUser, SlidersHorizontal, User, Users } from 'lucide-react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, type ReactNode } from 'react';
import { Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { ApiError, postForm } from './api/client';
import { getMe } from './api/me';
import { AdminUsersPage } from './pages/AdminUsersPage';
import { AdminConnectionConfigPage } from './pages/AdminConnectionConfigPage';
import { AdminSystemPage } from './pages/AdminSystemPage';
import { CommandsPage } from './pages/CommandsPage';
import { ConnectionsPage } from './pages/ConnectionsPage';
import { DashboardPage } from './pages/DashboardPage';
import { GeneratedPage } from './pages/GeneratedPage';
import { KnownUsersPage } from './pages/KnownUsersPage';
import { ProfilePage } from './pages/ProfilePage';
import { SendPage } from './pages/SendPage';
import { SystemPage } from './pages/SystemPage';
import { hasPermission, WEB_ADMIN_PERMISSION, WEB_USER_PERMISSION } from './permissions';

const navItems = [
  { label: 'System', path: '/', icon: Server },
  { label: 'Overview', path: '/overview', icon: Bot },
  { label: 'Known Users', path: '/users', icon: Users },
  { label: 'Connections', path: '/connections', icon: RadioTower },
  { label: 'Commands', path: '/commands', icon: ListTree },
];

const adminNavItems = [
  { label: 'Send', path: '/send', icon: Send },
  { label: 'Manage Users', path: '/admin/users', icon: ShieldUser },
  { label: 'Manage Connections', path: '/admin/config', icon: SlidersHorizontal },
  { label: 'Manage System', path: '/admin/system', icon: Settings },
];

export function App() {
  const location = useLocation();

  if (location.pathname.startsWith('/generated/')) {
    return (
      <Routes>
        <Route path="/generated/:id" element={<GeneratedPage />} />
      </Routes>
    );
  }

  return <AuthenticatedApp />;
}

function AuthenticatedApp() {
  const [opened, { close, toggle }] = useDisclosure();
  const location = useLocation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const meQuery = useQuery({
    queryKey: ['me'],
    queryFn: getMe,
    retry: false,
  });
  const authenticationRequired = meQuery.error instanceof ApiError && meQuery.error.authenticationRequired;

  useEffect(() => {
    if (authenticationRequired) {
      window.location.replace('/login');
    }
  }, [authenticationRequired]);

  const handleNavigate = (path: string) => {
    navigate(path);
    close();
  };

  const handleLogout = async () => {
    await postForm('/api/web/logout');
    queryClient.clear();
    window.location.replace(`${window.location.origin}/login`);
  };

  if (meQuery.isLoading || authenticationRequired) {
    return null;
  }
  const webAdmin = hasPermission(meQuery.data?.permissions, WEB_ADMIN_PERMISSION);
  const webUser = hasPermission(meQuery.data?.permissions, WEB_USER_PERMISSION);

  return (
    <AppShell
      header={{ height: 56 }}
      navbar={{
        width: 240,
        breakpoint: 'sm',
        collapsed: { mobile: !opened },
      }}
      padding={{ base: 'sm', sm: 'md' }}
    >
      <AppShell.Header className="app-header">
        <Group h="100%" px="md" justify="space-between">
          <Group gap="sm">
            <Burger opened={opened} onClick={toggle} hiddenFrom="sm" size="sm" />
            <Bot size={22} />
            <Title order={3}>the_bot</Title>
          </Group>
          <Group gap="sm">
            <Badge variant="light" visibleFrom="xs">web</Badge>
            <UserMenu
              username={meQuery.data?.username}
              name={meQuery.data?.name}
              webAdmin={webAdmin}
              onProfile={() => handleNavigate('/profile')}
              onLogout={handleLogout}
            />
          </Group>
        </Group>
      </AppShell.Header>

      <AppShell.Navbar p="sm">
        <Stack gap="xs">
          {webUser && (
            <NavSection
              label="User"
              items={navItems}
              activePath={location.pathname}
              onNavigate={handleNavigate}
            />
          )}
          {webAdmin && (
            <NavSection
              label="Admin"
              items={adminNavItems}
              activePath={location.pathname}
              onNavigate={handleNavigate}
            />
          )}
        </Stack>
      </AppShell.Navbar>

      <AppShell.Main>
        <Box className="page-frame">
          <Routes>
            <Route path="/" element={<RequireWebUser allowed={webUser}><SystemPage /></RequireWebUser>} />
            <Route path="/overview" element={<RequireWebUser allowed={webUser}><DashboardPage /></RequireWebUser>} />
            <Route path="/users" element={<RequireWebUser allowed={webUser}><KnownUsersPage /></RequireWebUser>} />
            <Route path="/commands" element={<RequireWebUser allowed={webUser}><CommandsPage /></RequireWebUser>} />
            <Route path="/send" element={<RequireWebAdmin allowed={webAdmin}><SendPage /></RequireWebAdmin>} />
            <Route path="/connections" element={<RequireWebUser allowed={webUser}><ConnectionsPage /></RequireWebUser>} />
            <Route path="/profile" element={<ProfilePage />} />
            <Route path="/admin/users" element={<RequireWebAdmin allowed={webAdmin}><AdminUsersPage /></RequireWebAdmin>} />
            <Route path="/admin/config" element={<RequireWebAdmin allowed={webAdmin}><AdminConnectionConfigPage /></RequireWebAdmin>} />
            <Route path="/admin/system" element={<RequireWebAdmin allowed={webAdmin}><AdminSystemPage /></RequireWebAdmin>} />
          </Routes>
        </Box>
      </AppShell.Main>
    </AppShell>
  );
}

type NavItem = {
  label: string;
  path: string;
  icon: typeof Server;
};

function NavSection({
  label,
  items,
  activePath,
  onNavigate,
}: {
  label: string;
  items: NavItem[];
  activePath: string;
  onNavigate: (path: string) => void;
}) {
  return (
    <Box>
      <Text size="xs" fw={700} c="dimmed" tt="uppercase" px="sm" mb={4}>
        {label}
      </Text>
      {items.map((item) => (
        <NavLink
          key={item.path}
          label={item.label}
          leftSection={<item.icon size={18} />}
          active={activePath === item.path}
          onClick={() => onNavigate(item.path)}
        />
      ))}
    </Box>
  );
}

function RequireWebUser({ allowed, children }: { allowed: boolean; children: ReactNode }) {
  if (!allowed) {
    return <AccessDenied title="Web access required" message="Your account does not have web user access." />;
  }
  return <>{children}</>;
}

function RequireWebAdmin({ allowed, children }: { allowed: boolean; children: ReactNode }) {
  if (!allowed) {
    return <AccessDenied title="Admin access required" message="This page is available only to web admins." />;
  }
  return <>{children}</>;
}

function AccessDenied({ title, message }: { title: string; message: string }) {
  return (
    <Alert color="red" variant="light" title={title}>
      {message}
    </Alert>
  );
}

function UserMenu({
  username,
  name,
  webAdmin,
  onProfile,
  onLogout,
}: {
  username?: string;
  name?: string | null;
  webAdmin?: boolean;
  onProfile: () => void;
  onLogout: () => void;
}) {
  if (!username) {
    return null;
  }

  const initials = (name || username).trim().slice(0, 1).toUpperCase();

  return (
    <Menu position="bottom-end" width={220} shadow="md">
      <Menu.Target>
        <UnstyledButton className="user-menu-button">
          <Group gap="xs" wrap="nowrap">
            <Avatar size="sm" radius="xl">{initials}</Avatar>
            <Box visibleFrom="sm" className="user-menu-label">
              <Text size="sm" fw={600} truncate>{name || username}</Text>
              <Text size="xs" c="dimmed" truncate>{webAdmin ? 'Web admin' : 'User'}</Text>
            </Box>
            <ChevronDown size={16} />
          </Group>
        </UnstyledButton>
      </Menu.Target>
      <Menu.Dropdown>
        <Menu.Label>{username}</Menu.Label>
        <Menu.Item leftSection={<User size={16} />} onClick={onProfile}>
          Your profile
        </Menu.Item>
        <Menu.Item leftSection={<Settings size={16} />} disabled>
          Account settings
        </Menu.Item>
        <Menu.Divider />
        <Menu.Item leftSection={<LogOut size={16} />} color="red" onClick={onLogout}>
          Sign out
        </Menu.Item>
      </Menu.Dropdown>
    </Menu>
  );
}
