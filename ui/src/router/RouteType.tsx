import { authStore } from "../store/AuthStore";
import { observer } from "mobx-react-lite";
import { Outlet, Navigate } from "react-router-dom";

interface ProtectedRouteProps {
    afterConfirmationOnly?: boolean;
}

export const ProtectedRoute = observer(({afterConfirmationOnly = true}: ProtectedRouteProps) => {
    const { user, isAuthenticated } = authStore;
    if (isAuthenticated && user) {
        if ((afterConfirmationOnly && user.isConfirmed)
            || (!afterConfirmationOnly && !user.isConfirmed)) {
            return <Outlet/>;
        }
        
    }
    
    let routeTo = '/login';
    if (user) {
        routeTo = user.isConfirmed ? '/home' : '/account-confirmation';
    }
    
    return <Navigate to={routeTo} replace />
});