import { AuthProvider } from "/context/AuthContext";
import { useInit } from "/context/InitContext";

export default function AuthWithReset({ children }) {
    const { resetUI } = useInit();
    
    return (
        <AuthProvider resetUI={resetUI}>
            {children}
        </AuthProvider>
    );
}

