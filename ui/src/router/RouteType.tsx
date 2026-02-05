import { authStore } from "../store/AuthStore";
import { observer } from "mobx-react-lite";
import { Outlet, Navigate } from "react-router-dom";

interface ProtectedRouteProps {
    afterConfirmationOnly?: boolean;
}

export const ProtectedRoute = observer(({afterConfirmationOnly = true}: ProtectedRouteProps) => {
    const { user, isAuthenticated, isLoading } = authStore;
    if (isLoading) {
        return <div>Loading...</div>
    }
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

export const NonAuthorizedRoute = observer(() => {
    const {user, isLoading} = authStore;
    if (isLoading) {
        return <div>Loading...</div>
    }
    if (user) {
        return <Navigate to={'/'} replace/>
    }

    return <Outlet />;
});