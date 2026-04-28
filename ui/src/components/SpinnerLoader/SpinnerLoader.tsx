import './SpinnerLoader.css';

interface OverlaySpinnerProps {
    text?: string;
}

export const OverlaySpinner = ({ text }: OverlaySpinnerProps) => {
    return (
        <div className="overlay-spinner-container">
            <div className="overlay-spinner-content">
                <div className="overlay-spinner"></div>
                {text && <span className="overlay-spinner-text">{text}</span>}
            </div>
        </div>
    );
};