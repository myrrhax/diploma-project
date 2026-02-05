import { createBrowserRouter, RouterProvider } from "react-router-dom";
import { LoginPage } from "../pages/LoginPage";
import { HomePage } from "../pages/HomePage";
import { NonAuthorizedRoute, ProtectedRoute } from "./RouteType";
import { Layout } from "../components/Layout";
import { authStore } from "../store/AuthStore";

const router = createBrowserRouter([
    {
        element: <NonAuthorizedRoute />,
        children: [
            {
                path: '/login',
                element: <LoginPage />
            },
        ]
    },
    {
        element: <Layout />,
        children: [
            {
                path: '/',
                element: <ProtectedRoute afterConfirmationOnly={false} />,
                children: [
                    {
                        element: <HomePage />
                    }
                ]
            },
            {
                element: <ProtectedRoute afterConfirmationOnly={true} />,
                children: [
                    { path: '/account-confirmation', element: <HomePage/> }
                ]
            }
        ]
    }
    
]);

export const AppRouter = () => {
    authStore.init();
    return (
        <RouterProvider router={router} />
    )
}