import { alpha, createTheme } from '@mui/material/styles';

/** The whole look in one place: colours, fonts, shapes and component defaults. */

const ink = '#2b2620'; // warm near-black for text
const seaGreen = '#4ba67a'; // primary: buttons, badges, accents
const bottleGreen = '#32835d'; // header bar and table titles
const brass = '#b5852f';
const page = '#faf9f6'; // warm off-white: a hint of paper, not beige
const paper = '#ffffff';
const rule = '#ebe6dc'; // thin borders and dividers

const serif = '"Playfair Display Variable", Georgia, "Times New Roman", serif';
const sans = '"Source Sans 3 Variable", "Helvetica Neue", Arial, sans-serif';

export const theme = createTheme({
  palette: {
    primary: { main: seaGreen, light: seaGreen, contrastText: paper },
    secondary: { main: brass, contrastText: '#fff' },
    error: { main: '#a63d32' },
    warning: { main: '#b26a00' },
    success: { main: '#3f6b3a' },
    background: { default: page, paper },
    text: { primary: ink, secondary: '#6b6257' },
    divider: rule,
  },
  shape: { borderRadius: 10 },
  typography: {
    fontFamily: sans,
    fontSize: 15,
    h1: { fontFamily: serif, fontWeight: 700 },
    h2: { fontFamily: serif, fontWeight: 700 },
    h3: { fontFamily: serif, fontWeight: 700 },
    h4: { fontFamily: serif, fontWeight: 700, letterSpacing: '-0.01em' },
    h5: { fontFamily: serif, fontWeight: 700 },
    h6: { fontFamily: serif, fontWeight: 700, lineHeight: 1.3 }, // book titles on the cards
    button: { textTransform: 'none', fontWeight: 600, letterSpacing: '0.01em' },
  },
  components: {
    MuiAppBar: {
      defaultProps: { elevation: 0 },
      styleOverrides: {
        root: { backgroundColor: bottleGreen, color: paper, borderBottom: `3px solid ${brass}` }, // white text at 4.6:1 contrast; brass rule under the header
      },
    },
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: { borderRadius: 999, paddingInline: 18 }, // pill-shaped buttons
      },
    },
    MuiCard: {
      defaultProps: { variant: 'outlined' },
      styleOverrides: {
        root: {
          borderColor: rule,
          transition: 'box-shadow 160ms ease, transform 160ms ease',
          '&:hover': { boxShadow: `0 8px 24px ${alpha(ink, 0.07)}`, transform: 'translateY(-2px)' },
          '@media (prefers-reduced-motion: reduce)': { transition: 'none', '&:hover': { transform: 'none' } },
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        outlined: { borderColor: rule },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: { fontWeight: 600 },
      },
    },
    MuiTableHead: {
      styleOverrides: {
        root: {
          backgroundColor: alpha(bottleGreen, 0.04),
          '& .MuiTableCell-head': { fontWeight: 700, color: bottleGreen },
        },
      },
    },
    MuiTableCell: {
      styleOverrides: {
        // tighter cells on phones, so tables don't scroll sideways
        root: ({ theme }) => ({
          borderColor: rule,
          [theme.breakpoints.down('sm')]: { paddingLeft: 8, paddingRight: 8 },
        }),
      },
    },
    MuiTableFooter: {
      styleOverrides: {
        root: { '& .MuiTableCell-footer': { color: ink, fontSize: '1rem' } },
      },
    },
    MuiBadge: {
      styleOverrides: {
        badge: { fontWeight: 700 },
      },
    },
    MuiAlert: {
      styleOverrides: {
        root: { borderRadius: 10 },
      },
    },
    MuiPaginationItem: {
      styleOverrides: {
        root: { fontWeight: 600 },
      },
    },
  },
});
