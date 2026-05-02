import { AppShell, Badge, Box, Burger, Group, NavLink, Text, Title } from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { Bot, RadioTower, Send, Users } from 'lucide-react';
import { Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { DashboardPage } from './pages/DashboardPage';

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

  const handleNavigate = (path: string) => {
    navigate(path);
    close();
  };

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
          <Badge variant="light">web</Badge>
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
            <Route path="/users" element={<Placeholder title="Known Users" />} />
            <Route path="/send" element={<Placeholder title="Send Message" />} />
            <Route path="/connections" element={<Placeholder title="Connections" />} />
          </Routes>
        </Box>
      </AppShell.Main>
    </AppShell>
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
