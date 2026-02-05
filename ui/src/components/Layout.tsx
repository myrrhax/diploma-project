import { Outlet } from "react-router-dom";
import { Header } from "./Header/Header";

export const Layout = () => {
    return (
        <>
            <Header />
            <main style={{minHeight: '100vh', display: 'flex', flexDirection: 'column'}}>
                <Outlet />
            </main>
        </>
    );
}