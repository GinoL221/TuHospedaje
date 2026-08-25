import { lazy, Suspense } from "react";
import { BrowserRouter, Routes, Route, useLocation, Navigate } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";

import RequireAuth from "./components/RequireAuth";
import RequireAdmin from "./components/RequireAdmin";
import Header from "./components/Header/Header";
import Footer from "./components/Footer/Footer";
import RouteChunkErrorBoundary from "./components/RouteChunkErrorBoundary";
import RouteLoadingFallback from "./components/RouteLoadingFallback";

const Home = lazy(() => import("./pages/Home/Home"));
const LoginPage = lazy(() => import("./pages/LoginPage"));
const RegisterPage = lazy(() => import("./pages/RegisterPage"));
const ProductDetail = lazy(() => import("./pages/ProductDetail/ProductDetail"));
const Admin = lazy(() => import("./pages/Admin/Admin"));
const FavoritesPage = lazy(() => import("./pages/Favorites/FavoritesPage"));
const BookingPage = lazy(() => import("./pages/Booking/BookingPage"));
const BookingConfirmationPage = lazy(() => import("./pages/Booking/BookingConfirmation"));
const MyReservationsPage = lazy(() => import("./pages/MyReservations/MyReservationsPage"));
const Unauthorized = lazy(() => import("./pages/Unauthorized/Unauthorized"));

// Canonical administration route plus its compatibility alias. Both are
// checked so the shell suppressor never flashes Header/Footer while the
// alias redirects to the canonical destination.
const ADMIN_PATH_PREFIXES = ["/administración", "/admin"];
function isAdminPath(pathname) {
  return ADMIN_PATH_PREFIXES.some((prefix) => pathname.startsWith(prefix));
}

function AppLayout() {
	const location = useLocation();
	const { pathname } = location;
  const isAdmin = isAdminPath(pathname);

  return (
    <>
      {!isAdmin && <Header />}
      <RouteChunkErrorBoundary resetKey={pathname}>
        <Suspense fallback={<RouteLoadingFallback />}>
          <Routes>
            <Route path="/" element={<Home />} />
				<Route path="/search" element={<Navigate to={{ pathname: "/", search: location.search }} replace />} />
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
              <Route path="/administración" element={<Admin />} />
              <Route
                path="/admin"
                element={<Navigate to="/administración" replace />}
              />
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
