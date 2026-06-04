import { useState } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { Sun, Moon, Menu, X } from 'lucide-react';
import { useTheme } from '../../hooks/useTheme';
import styles from './Layout.module.css';

export default function Layout() {
  const { theme, toggleTheme } = useTheme();
  const [menuOpen, setMenuOpen] = useState(false);

  const closeMenu = () => setMenuOpen(false);

  return (
    <>
      <nav className={styles.navbar}>
        <NavLink to="/" className={styles.brand} onClick={closeMenu}>
          <div className={styles.logo}>F1</div>
          <span>MessageTool</span>
        </NavLink>

        <div className={styles.desktopNav}>
          <NavLink to="/" end className={({ isActive }) => `${styles.link} ${isActive ? styles.active : ''}`}>
            Calendar
          </NavLink>
          <NavLink to="/results" className={({ isActive }) => `${styles.link} ${isActive ? styles.active : ''}`}>
            Results
          </NavLink>
          <NavLink to="/drivers" className={({ isActive }) => `${styles.link} ${isActive ? styles.active : ''}`}>
            Drivers
          </NavLink>
          <NavLink to="/notes" className={({ isActive }) => `${styles.link} ${isActive ? styles.active : ''}`}>
            Notes
          </NavLink>
          <button className={styles.themeToggle} onClick={toggleTheme} aria-label="Toggle theme">
            {theme === 'dark' ? <Moon size={18} /> : <Sun size={18} />}
          </button>
        </div>

        <button className={styles.hamburger} onClick={() => setMenuOpen(!menuOpen)} aria-label="Toggle menu">
          {menuOpen ? <X size={22} /> : <Menu size={22} />}
        </button>
      </nav>

      {menuOpen && (
        <div className={styles.mobileMenu}>
          <NavLink to="/" end className={({ isActive }) => `${styles.mobileLink} ${isActive ? styles.active : ''}`} onClick={closeMenu}>
            Calendar
          </NavLink>
          <NavLink to="/results" className={({ isActive }) => `${styles.mobileLink} ${isActive ? styles.active : ''}`} onClick={closeMenu}>
            Results
          </NavLink>
          <NavLink to="/drivers" className={({ isActive }) => `${styles.mobileLink} ${isActive ? styles.active : ''}`} onClick={closeMenu}>
            Drivers
          </NavLink>
          <NavLink to="/notes" className={({ isActive }) => `${styles.mobileLink} ${isActive ? styles.active : ''}`} onClick={closeMenu}>
            Notes
          </NavLink>
          <button className={styles.mobileThemeToggle} onClick={toggleTheme}>
            {theme === 'dark' ? <Moon size={16} /> : <Sun size={16} />}
            {theme === 'dark' ? 'Dark Mode' : 'Light Mode'}
          </button>
        </div>
      )}

      <div className={styles.container}>
        <Outlet />
      </div>
    </>
  );
}
