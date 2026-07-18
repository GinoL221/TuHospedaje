import { lazy, Suspense } from "react";
import { BrowserRouter, Routes, Route, useLocation } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";

import RequireAuth from "./components/RequireAuth";
import RequireAdmin from "./components/RequireAdmin";
import Header from "./components/Header/Header";
import Footer from "./components/Footer/Footer";
import RouteChunkErrorBoundary from "./components/RouteChunkErrorBoundary";
import RouteLoadingFallback from "./components/RouteLoadingFallback";
import WhatsAppButton from "./components/WhatsAppButton/WhatsAppButton";

const Home = lazy(() => import("./pages/Home/Home"));
const LoginPage = lazy(() => import("./pages/LoginPage"));
const RegisterPage = lazy(() => import("./pages/RegisterPage"));
const ProductDetail = lazy(() => import("./pages/ProductDetail/ProductDetail"));
const Admin = lazy(() => import("./pages/Admin/Admin"));
const SearchResults = lazy(() => import("./pages/SearchResults/SearchResults"));
const FavoritesPage = lazy(() => import("./pages/Favorites/FavoritesPage"));
const BookingPage = lazy(() => import("./pages/Booking/BookingPage"));
const BookingConfirmationPage = lazy(() => import("./pages/Booking/BookingConfirmation"));
const MyReservationsPage = lazy(() => import("./pages/MyReservations/MyReservationsPage"));
const Unauthorized = lazy(() => import("./pages/Unauthorized/Unauthorized"));

function AppLayout() {
  const { pathname } = useLocation();
  const isAdmin = pathname.startsWith("/admin");

  return (
    <>
      {!isAdmin && <Header />}
      {!isAdmin && <WhatsAppButton />}
      <RouteChunkErrorBoundary resetKey={pathname}>
        <Suspense fallback={<RouteLoadingFallback />}>
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
        </Suspense>
      </RouteChunkErrorBoundary>
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
