import { participationApiService } from "@/api/ParticipationApiService";
import { OverlaySpinner } from "@/components/SpinnerLoader/SpinnerLoader";
import { useEffect, useRef } from "react";
import { useSearchParams, useNavigate } from "react-router-dom"

export const InvitationsPage = () => {
    console.log('INVITATION PAGE');
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const confirmId = searchParams.get('confirm');

    const hasProcessed = useRef(false);

    useEffect(() => {
        if (!confirmId) {
            navigate('/', { replace: true });
            console.log('NO CONFIRMATION');
            return;
        }

        if (hasProcessed.current) return;
        hasProcessed.current = true;

        const processInvitation = async () => {
            try {
                const processedInvitation = await participationApiService.confirmInvitation(confirmId);
                
                console.log('PROCESSED', processInvitation);
                navigate(`/schema/edit/${processedInvitation.schemaId}`);
                
            } catch (error) {
                console.log('GOT ERROR', error);
                navigate('/', {
                    replace: true,
                    state: {
                        invitationError: 'Не удалось принять приглашение. Возможно, ссылка устарела или уже была использована.'
                    }
                })
            }
        };

        processInvitation();

    }, [confirmId, navigate]);

    return (
        <OverlaySpinner text="Загрузка..." />
    )
}