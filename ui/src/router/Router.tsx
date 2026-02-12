import { createBrowserRouter, RouterProvider } from "react-router-dom";
import { LoginPage } from "../pages/LoginPage";
import { HomePage } from "../pages/HomePage";
import { NonAuthorizedRoute, ProtectedRoute } from "./RouteType";
import { Layout } from "../components/Layout";
import { authStore } from "../store/AuthStore";
import { RegisterPage } from "../pages/RegisterPage";
import { AccountConfirmationPage } from "../pages/AccountConfirmationPage";
import { SchemaEditorPage } from "../pages/SchemaEditorPage";

const router = createBrowserRouter([
    {
        element: <NonAuthorizedRoute />,
        children: [
            {
                path: '/login',
                element: <LoginPage />
            },
            {
                path: '/register',
                element: <RegisterPage />
            }
        ]
    },
    {
        element: <ProtectedRoute afterConfirmationOnly={false} />,
        children: [
            { path: '/account-confirmation', element: <AccountConfirmationPage/> }
        ]
    },
    {
        element: <Layout />,
        children: [
            {
                path: '/',
                element: <ProtectedRoute afterConfirmationOnly={true} />,
                children: [
                    {
                        path: '/',
                        element: <HomePage />
                    },
                    {
                        path: '/schema/edit/:id',
                        element: <SchemaEditorPage />
                    }
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