import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import { alpha } from '@mui/material/styles';

export function HeroBanner() {
  return (
    <Box
      sx={(theme) => ({
        mb: 4,
        px: { xs: 3, md: 5 },
        py: { xs: 4, md: 5 },
        borderRadius: 3,
        color: theme.palette.primary.contrastText,
        background: `linear-gradient(120deg, ${theme.palette.primary.dark}, ${theme.palette.primary.main} 60%, ${alpha(theme.palette.secondary.main, 0.85)})`,
      })}
    >
      <Typography
        component="p"
        sx={(theme) => ({
          fontFamily: theme.typography.h1.fontFamily,
          fontWeight: 700,
          fontSize: { xs: '1.8rem', md: '2.6rem' },
          lineHeight: 1.15,
        })}
      >
        Find your next great read
      </Typography>
      <Typography sx={{ mt: 1.5, maxWidth: 560, opacity: 0.9, fontSize: '1.05rem' }}>
        Hand-picked books on software craft, architecture and the way teams work.
      </Typography>
    </Box>
  );
}
