import { Routes, Route } from 'react-router-dom';
import Layout from './components/Layout/Layout';
import Calendar from './pages/Calendar/Calendar';
import RaceReport from './pages/RaceReport/RaceReport';
import Results from './pages/Results/Results';
import Drivers from './pages/Drivers/Drivers';
import DriverDetail from './pages/DriverDetail/DriverDetail';
import Notes from './pages/Notes/Notes';
import NoteEditor from './pages/NoteEditor/NoteEditor';
import './index.css';

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Calendar />} />
        <Route path="/calendar" element={<Calendar />} />
        <Route path="/calendar/:meetingKey" element={<RaceReport />} />
        <Route path="/results" element={<Results />} />
        <Route path="/drivers" element={<Drivers />} />
        <Route path="/drivers/:driverNumber" element={<DriverDetail />} />
        <Route path="/notes" element={<Notes />} />
        <Route path="/notes/new" element={<NoteEditor />} />
        <Route path="/notes/:id" element={<NoteEditor />} />
      </Route>
    </Routes>
  );
}
