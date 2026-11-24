import { useId, useState } from 'react';
import Avatar from '@mui/material/Avatar';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import ListItemIcon from '@mui/material/ListItemIcon';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import LogoutIcon from '@mui/icons-material/Logout';
import ReceiptLongIcon from '@mui/icons-material/ReceiptLongOutlined';
import { Link as RouterLink } from 'react-router-dom';
import type { User } from '@/features/auth';

interface AccountMenuProps {
  user: User;
  onLogout: () => void;
}

/** One account menu instead of separate header buttons, which wrapped on phones. */
export function AccountMenu({ user, onLogout }: AccountMenuProps) {
  const [anchor, setAnchor] = useState<HTMLElement | null>(null);
  const menuId = useId();
  const open = anchor !== null;
  const close = () => setAnchor(null);

  return (
    <>
      <Button
        color="inherit"
        // the name is hidden on phones, so the accessible name carries it
        aria-label={`Account: ${user.fullName}`}
        aria-haspopup="true"
        aria-expanded={open}
        aria-controls={open ? menuId : undefined}
        onClick={(event) => setAnchor(event.currentTarget)}
        startIcon={
          <Avatar
            aria-hidden
            sx={{ width: 30, height: 30, bgcolor: 'secondary.main', fontSize: '0.95rem', fontWeight: 700 }}
          >
            {user.fullName.charAt(0).toUpperCase()}
          </Avatar>
        }
      >
        <Box component="span" sx={{ display: { xs: 'none', sm: 'inline' } }}>
          {user.fullName}
        </Box>
      </Button>
      <Menu
        id={menuId}
        anchorEl={anchor}
        open={open}
        onClose={close}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
      >
        <MenuItem component={RouterLink} to="/orders" onClick={close}>
          <ListItemIcon>
            <ReceiptLongIcon fontSize="small" />
          </ListItemIcon>
          My orders
        </MenuItem>
        <MenuItem
          onClick={() => {
            close();
            onLogout();
          }}
        >
          <ListItemIcon>
            <LogoutIcon fontSize="small" />
          </ListItemIcon>
          Log out
        </MenuItem>
      </Menu>
    </>
  );
}
