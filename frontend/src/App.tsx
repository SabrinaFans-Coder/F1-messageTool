import { Routes, Route } from 'react-router-dom';
import Layout from './components/Layout/Layout';
import Home from './pages/Home/Home';
import Calendar from './pages/Calendar/Calendar';
import RaceReport from './pages/RaceReport/RaceReport';
import Results from './pages/Results/Results';
import Drivers from './pages/Drivers/Drivers';
import DriverDetail from './pages/DriverDetail/DriverDetail';
import Teams from './pages/Teams/Teams';
import TeamDetail from './pages/TeamDetail/TeamDetail';
import News from './pages/News/News';
import Trends from './pages/Trends/Trends';
import './index.css';

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Home />} />
        <Route path="/news" element={<News />} />
        <Route path="/calendar" element={<Calendar />} />
        <Route path="/calendar/:meetingKey" element={<RaceReport />} />
        <Route path="/results" element={<Results />} />
        <Route path="/trends" element={<Trends />} />
        <Route path="/drivers" element={<Drivers />} />
        <Route path="/drivers/:driverNumber" element={<DriverDetail />} />
        <Route path="/teams" element={<Teams />} />
        <Route path="/teams/:teamName" element={<TeamDetail />} />
      </Route>
    </Routes>
  );
}
