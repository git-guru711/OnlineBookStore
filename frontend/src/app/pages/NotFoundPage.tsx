import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { Link as RouterLink } from 'react-router-dom';
import { HOME_PATH } from '@/lib/paths';

export function NotFoundPage() {
  return (
    <Stack spacing={2} sx={{ alignItems: 'flex-start' }}>
      <Typography variant="h4" component="h1">
        Page not found
      </Typography>
      <Typography color="text.secondary">The page you are looking for does not exist.</Typography>
      <Button variant="contained" component={RouterLink} to={HOME_PATH}>
        Back to the bookstore
      </Button>
    </Stack>
  );
}
