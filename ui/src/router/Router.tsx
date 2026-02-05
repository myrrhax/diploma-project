import { createBrowserRouter, RouterProvider } from "react-router-dom";
import { LoginPage } from "../pages/LoginPage";
import { HomePage } from "../pages/HomePage";
import { ProtectedRoute } from "./RouteType";
import { Layout } from "../components/Layout";

const router = createBrowserRouter([
    {
        path: '/login',
        element: <LoginPage />
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
                    { path: 'account-confirmation', element: <HomePage/> }
                ]
            }
        ]
    }
    
]);

export const AppRouter = () => {
    return (
        <RouterProvider router={router} />
    )
}