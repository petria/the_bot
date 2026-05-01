import { AppShell, Badge, Group, NavLink, Text, Title } from '@mantine/core';
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
  const location = useLocation();
  const navigate = useNavigate();

  return (
    <AppShell
      header={{ height: 56 }}
      navbar={{ width: 240, breakpoint: 'sm' }}
      padding="md"
    >
      <AppShell.Header className="app-header">
        <Group h="100%" px="md" justify="space-between">
          <Group gap="sm">
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
            onClick={() => navigate(item.path)}
          />
        ))}
      </AppShell.Navbar>

      <AppShell.Main>
        <Routes>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/users" element={<Placeholder title="Known Users" />} />
          <Route path="/send" element={<Placeholder title="Send Message" />} />
          <Route path="/connections" element={<Placeholder title="Connections" />} />
        </Routes>
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
