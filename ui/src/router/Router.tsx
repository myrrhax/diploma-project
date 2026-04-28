import { createBrowserRouter, RouterProvider } from "react-router-dom";
import { LoginPage } from "../pages/LoginPage";
import { HomePage } from "../pages/HomePage";
import { NonAuthorizedRoute, ProtectedRoute } from "./RouteType";
import { Layout } from "../components/Layout";
import { authStore } from "../store/AuthStore";
import { RegisterPage } from "../pages/RegisterPage";
import { AccountConfirmationPage } from "../pages/AccountConfirmationPage";
import { SchemaEditorPage } from "../pages/SchemaEditorPage";
import { observer } from "mobx-react-lite";
import { useEffect } from "react";
import { InvitationsPage } from "@/pages/InvitationsPage";

const router = createBrowserRouter([
    {
        element: <Layout />,
        path: '/',
        children: [
            {
                element: <ProtectedRoute afterConfirmationOnly={true} />,
                children: [
                    {
                        path: '/',
                        element: <HomePage />
                    },
                    {
                        path: '/schema/edit/:id',
                        element: <SchemaEditorPage />
                    },
                    {
                        path: '/schema/:id/version/:versionId',
                        element: <SchemaEditorPage isReadonly={true} />
                    },
                    {
                        path: '/invitations',
                        element: <InvitationsPage />
                    }
                ]
            }
        ]
    },
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
]);

export const AppRouter = observer(() => {    
    useEffect(() => {
        authStore.init();
    }, []); 
    
    return <RouterProvider router={router} />;
})