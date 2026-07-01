import { BrowserRouter, Routes, Route, useLocation } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";

import RequireAuth from "./components/RequireAuth";
import RequireAdmin from "./components/RequireAdmin";
import Header from "./components/Header/Header";
import Footer from "./components/Footer/Footer";
import Home from "./pages/Home/Home";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import ProductDetail from "./pages/ProductDetail/ProductDetail";
import Admin from "./pages/Admin/Admin";
import SearchResults from "./pages/SearchResults/SearchResults";
import FavoritesPage from "./pages/Favorites/FavoritesPage";
import BookingPage from "./pages/Booking/BookingPage";
import BookingConfirmationPage from "./pages/Booking/BookingConfirmation";
import MyReservationsPage from "./pages/MyReservations/MyReservationsPage";
import Unauthorized from "./pages/Unauthorized/Unauthorized";
import WhatsAppButton from "./components/WhatsAppButton/WhatsAppButton";

function AppLayout() {
  const { pathname } = useLocation();
  const isAdmin = pathname.startsWith("/admin");

  return (
    <>
      {!isAdmin && <Header />}
      {!isAdmin && <WhatsAppButton />}
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/search" element={<SearchResults />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/lodgings/:id" element={<ProductDetail />} />
        <Route path="/favorites" element={<FavoritesPage />} />
        <Route path="/unauthorized" element={<Unauthorized />} />

        <Route element={<RequireAuth />}>
          <Route path="/booking/:lodgingId" element={<BookingPage />} />
          <Route path="/booking/confirmation" element={<BookingConfirmationPage />} />
          <Route path="/my-reservations" element={<MyReservationsPage />} />
        </Route>

        <Route element={<RequireAdmin />}>
          <Route path="/admin" element={<Admin />} />
        </Route>
      </Routes>
      {!isAdmin && <Footer />}
    </>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppLayout />
      </AuthProvider>
    </BrowserRouter>
  );
}
