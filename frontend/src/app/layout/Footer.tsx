import Box from '@mui/material/Box';
import Container from '@mui/material/Container';
import Typography from '@mui/material/Typography';

export function Footer() {
  return (
    <Box component="footer" sx={{ mt: 'auto', borderTop: 1, borderColor: 'divider', bgcolor: 'background.paper' }}>
      <Container
        maxWidth="lg"
        sx={{ py: 3, display: 'flex', flexWrap: 'wrap', gap: 1, justifyContent: 'space-between' }}
      >
        <Typography variant="body2" color="text.secondary">
          © {new Date().getFullYear()} Bookstore · Books for curious minds
        </Typography>
        <Typography variant="body2" color="text.secondary">
          A demo shop built with React and Spring Boot
        </Typography>
      </Container>
    </Box>
  );
}
