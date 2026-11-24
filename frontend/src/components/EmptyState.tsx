import type { ReactNode } from 'react';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import { alpha } from '@mui/material/styles';

interface EmptyStateProps {
  icon: ReactNode;
  title: string;
  description?: string;
  action?: ReactNode;
}

export function EmptyState({ icon, title, description, action }: EmptyStateProps) {
  return (
    <Box sx={{ textAlign: 'center', py: { xs: 5, md: 8 }, px: 2 }}>
      <Box
        aria-hidden
        sx={(theme) => ({
          width: 96,
          height: 96,
          mx: 'auto',
          mb: 2,
          borderRadius: '50%',
          display: 'grid',
          placeItems: 'center',
          color: theme.palette.primary.main,
          bgcolor: alpha(theme.palette.primary.main, 0.07),
          '& svg': { fontSize: 48 },
        })}
      >
        {icon}
      </Box>
      <Typography variant="h6" component="p" gutterBottom>
        {title}
      </Typography>
      {description && (
        <Typography color="text.secondary" sx={{ maxWidth: 420, mx: 'auto' }}>
          {description}
        </Typography>
      )}
      {action && <Box sx={{ mt: 3 }}>{action}</Box>}
    </Box>
  );
}
