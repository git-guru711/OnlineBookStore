import { useState } from 'react';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import { alpha } from '@mui/material/styles';

/** "?default=false" turns a missing cover into a 404 instead of a blank image. */
function coverUrl(isbn: string): string {
  return `https://covers.openlibrary.org/b/isbn/${isbn}-M.jpg?default=false`;
}

interface BookCoverProps {
  isbn: string;
  title: string;
}

/** Falls back to a drawn cover with the title when there is no image, so no broken-image icon. */
export function BookCover({ isbn, title }: BookCoverProps) {
  const [failed, setFailed] = useState(false);
  const label = `Cover of ${title}`;

  return (
    <Box
      sx={(theme) => ({
        height: 210,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        py: 2,
        bgcolor: alpha(theme.palette.primary.main, 0.04),
        borderBottom: `1px solid ${theme.palette.divider}`,
      })}
    >
      {failed ? (
        <Box
          role="img"
          aria-label={label}
          sx={(theme) => ({
            height: '100%',
            aspectRatio: '2 / 3',
            p: 1.5,
            display: 'flex',
            alignItems: 'center',
            borderRadius: '2px 6px 6px 2px',
            color: theme.palette.primary.contrastText,
            background: `linear-gradient(135deg, ${theme.palette.primary.main}, ${theme.palette.primary.dark})`,
            borderLeft: `6px solid ${alpha(theme.palette.common.black, 0.25)}`, // the spine
            boxShadow: `0 4px 12px ${alpha(theme.palette.common.black, 0.18)}`,
          })}
        >
          <Typography
            aria-hidden
            sx={(theme) => ({
              fontFamily: theme.typography.h6.fontFamily,
              fontWeight: 700,
              fontSize: '0.95rem',
              lineHeight: 1.25,
              textAlign: 'center',
              width: '100%',
              borderTop: `1px solid ${theme.palette.secondary.main}`,
              borderBottom: `1px solid ${theme.palette.secondary.main}`,
              py: 1,
            })}
          >
            {title}
          </Typography>
        </Box>
      ) : (
        <Box
          component="img"
          src={coverUrl(isbn)}
          alt={label}
          loading="lazy"
          onError={() => setFailed(true)}
          sx={(theme) => ({
            height: '100%',
            maxWidth: '100%',
            objectFit: 'contain',
            borderRadius: '2px 4px 4px 2px',
            boxShadow: `0 4px 12px ${alpha(theme.palette.common.black, 0.18)}`,
          })}
        />
      )}
    </Box>
  );
}
