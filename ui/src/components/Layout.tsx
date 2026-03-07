import { Outlet } from "react-router-dom";
import { Header } from "./Header/Header";
import { observer } from "mobx-react-lite";
import { authStore } from "@/store/AuthStore";
import { OverlaySpinner } from "./SpinnerLoader/SpinnerLoader";

export const Layout = observer(() => {
    const { isLoading } = authStore;

    if (isLoading) {
        return <OverlaySpinner text="Инициализация приложения..." />;
    }
    
    return (
        <div style={{ height: '100vh', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
            <Header />
            <main style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
                <Outlet />
            </main>
        </div>
    );
})