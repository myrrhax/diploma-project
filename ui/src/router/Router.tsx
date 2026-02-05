import { createBrowserRouter } from "react-router-dom";
import { LoginPage } from "../pages/LoginPage";
import { HomePage } from "../pages/HomePage";
import { ProtectedRoute } from "./RouteType";

export const router = createBrowserRouter([
    {
        path: '/login',
        element: <LoginPage />
    },
    {
        element: <ProtectedRoute afterConfirmationOnly={true} />,
        children: [
            { path: 'account-confirmation', element: <HomePage/> }
        ]
    }
]);
