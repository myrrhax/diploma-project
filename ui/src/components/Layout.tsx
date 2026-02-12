import { Outlet } from "react-router-dom";
import { Header } from "./Header/Header";

export const Layout = () => {
    return (
        <div style={{ height: '100vh', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
            <Header />
            <main style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
                <Outlet />
            </main>
        </div>
    );
}