import { AppShell, Avatar, Badge, Box, Burger, Group, Menu, NavLink, Text, Title, UnstyledButton } from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { Bot, ChevronDown, LogOut, RadioTower, Send, Settings, User, Users } from 'lucide-react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { ApiError, postForm } from './api/client';
import { getMe } from './api/me';
import { DashboardPage } from './pages/DashboardPage';
import { KnownUsersPage } from './pages/KnownUsersPage';
import { ProfilePage } from './pages/ProfilePage';

const navItems = [
  { label: 'Overview', path: '/', icon: Bot },
  { label: 'Known Users', path: '/users', icon: Users },
  { label: 'Send', path: '/send', icon: Send },
  { label: 'Connections', path: '/connections', icon: RadioTower },
];

export function App() {
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
              admin={meQuery.data?.admin}
              onProfile={() => handleNavigate('/profile')}
              onLogout={handleLogout}
            />
          </Group>
        </Group>
      </AppShell.Header>

      <AppShell.Navbar p="sm">
        {navItems.map((item) => (
          <NavLink
            key={item.path}
            label={item.label}
            leftSection={<item.icon size={18} />}
            active={location.pathname === item.path}
            onClick={() => handleNavigate(item.path)}
          />
        ))}
      </AppShell.Navbar>

      <AppShell.Main>
        <Box className="page-frame">
          <Routes>
            <Route path="/" element={<DashboardPage />} />
            <Route path="/users" element={<KnownUsersPage />} />
            <Route path="/send" element={<Placeholder title="Send Message" />} />
            <Route path="/connections" element={<Placeholder title="Connections" />} />
            <Route path="/profile" element={<ProfilePage />} />
          </Routes>
        </Box>
      </AppShell.Main>
    </AppShell>
  );
}

function UserMenu({
  username,
  name,
  admin,
  onProfile,
  onLogout,
}: {
  username?: string;
  name?: string | null;
  admin?: boolean;
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
              <Text size="xs" c="dimmed" truncate>{admin ? 'Admin' : 'User'}</Text>
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

function Placeholder({ title }: { title: string }) {
  return (
    <>
      <Title order={2}>{title}</Title>
      <Text c="dimmed" mt="xs">This page will be wired to bot APIs in the next step.</Text>
    </>
  );
}
