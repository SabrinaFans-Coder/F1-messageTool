import { NavLink, Outlet } from 'react-router-dom';
import { Sun, Moon } from 'lucide-react';
import { useTheme } from '../../hooks/useTheme';
import styles from './Layout.module.css';

export default function Layout() {
  const { theme, toggleTheme } = useTheme();

  return (
    <>
      <nav className={styles.navbar}>
        <NavLink to="/" className={styles.brand}>
          <div className={styles.logo}>F1</div>
          <span>MessageTool</span>
        </NavLink>
        <div className={styles.nav}>
          <NavLink
            to="/"
            end
            className={({ isActive }) =>
              `${styles.link} ${isActive ? styles.active : ''}`
            }
          >
            Calendar
          </NavLink>
          <NavLink
            to="/results"
            className={({ isActive }) =>
              `${styles.link} ${isActive ? styles.active : ''}`
            }
          >
            Results
          </NavLink>
          <NavLink
            to="/drivers"
            className={({ isActive }) =>
              `${styles.link} ${isActive ? styles.active : ''}`
            }
          >
            Drivers
          </NavLink>
          <NavLink
            to="/notes"
            className={({ isActive }) =>
              `${styles.link} ${isActive ? styles.active : ''}`
            }
          >
            Notes
          </NavLink>
          <button
            className={styles.themeToggle}
            onClick={toggleTheme}
            aria-label="Toggle theme"
          >
            {theme === 'dark' ? <Moon size={18} /> : <Sun size={18} />}
          </button>
        </div>
      </nav>
      <div className={styles.container}>
        <Outlet />
      </div>
    </>
  );
}
