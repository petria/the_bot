import { Button, Stack, Text, Title } from '@mantine/core';
import { Link } from 'react-router-dom';
import { AlertCircle } from 'lucide-react';

export function NotFoundPage() {
  return (
    <Stack gap="md">
      <AlertCircle size={34} />
      <div>
        <Title order={2}>Page not found</Title>
        <Text c="dimmed" mt={4}>
          The page does not exist or the link is no longer valid.
        </Text>
      </div>
      <Button component={Link} to="/" w="fit-content">
        Back to System
      </Button>
    </Stack>
  );
}
