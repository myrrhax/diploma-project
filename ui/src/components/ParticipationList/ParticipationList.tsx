import { participationsStore } from "@/store/ParticipationStore";
import { observer } from "mobx-react-lite";
import './css/ParticipationList.css';
import closeIcon from '@/assets/close.svg';
import profilePic from '@/assets/user.png';
import { useRef, useState, type MouseEvent } from "react";
import type { Participation } from "@/model/Participation";
import { ParticipationInfoTooltip } from "./ParticipationInfoTooltip";

const BLUR_TIMEOUT_MS = 500;

export const ParticipationList = observer(() => {
    const { participations, isListModalOpen } = participationsStore;
    const [hoveredUser, setHoveredUser] = useState<Participation | null>(null);
    const [top, setTop] = useState<number | null>(null);
    const [left, setLeft] = useState<number | null>(null);

    const timeoutRef = useRef<number | null>(null);
    const modalRef = useRef<HTMLDivElement>(null);

    if (!isListModalOpen) {
        return;
    }

    const changeHoveredUser = (p: Participation, e: MouseEvent<HTMLDivElement>) => {
        if (timeoutRef.current) {
            onBlurCancel();
        }
        
        if (modalRef.current) {
            setHoveredUser(p);
            const modalRect = modalRef.current?.getBoundingClientRect(); 
            setLeft(modalRect.right + 10);

            const elRect = e.currentTarget.getBoundingClientRect();
            setTop(elRect.top);
        }
        
    };

    const closeTooltip = () => {
        setHoveredUser(null);
        setLeft(null);
        setTop(null);
    }

    const startBlurTimer = () => {
        timeoutRef.current = setTimeout(() => {
            closeTooltip();
            timeoutRef.current = null;
        }, BLUR_TIMEOUT_MS);
    }

    const onBlurCancel = () => {
        if (timeoutRef.current) {
            clearTimeout(timeoutRef.current);
            timeoutRef.current = null;
        }
    }

    return (
        <div className="participation_list__overlay" 
            onClick={() => participationsStore.closeListModal()}
        >
            <div className="participation_list__container"
                ref={modalRef}
                onClick={(e: MouseEvent<HTMLDivElement>) => e.stopPropagation()}
            >
                <div className="participation_list__header">
                    <h3>Участники</h3>
                    <img src={ closeIcon } onClick={() => participationsStore.closeListModal()} />
                </div>

                <div className="participation_list__content">
                    {participations.map((p, i) => (
                        <div className="participation_info" key={'participation_' + i}
                            onMouseEnter={(e: MouseEvent<HTMLDivElement>) => changeHoveredUser(p, e)}
                            onMouseLeave={() => startBlurTimer()}
                        >
                            <img src={profilePic} />

                            <span className="participation_info__email">{p.user.email}</span>
                        </div>
                    ))}
                </div>
            </div>
                {hoveredUser ? (
                    <ParticipationInfoTooltip 
                        key={hoveredUser.user.id}
                        left={left!!}
                        top={top!!}
                        participation={hoveredUser} 
                        cancelTimeout={onBlurCancel}
                        onLeave={closeTooltip} 
                    />
                ) : null}
        </div>
    );
});