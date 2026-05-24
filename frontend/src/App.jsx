import { BrowserRouter, Routes, Route } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";

import Header from "./components/Header/Header";
import Footer from "./components/Footer/Footer";
import Home from "./pages/Home/Home";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import ProductDetail from "./pages/ProductDetail/ProductDetail";
import Admin from "./pages/Admin/Admin";

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Header />
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/lodgings/:id" element={<ProductDetail />} />
          <Route path="/admin" element={<Admin />} />
        </Routes>
        <Footer />
      </AuthProvider>
    </BrowserRouter>
  );
}
