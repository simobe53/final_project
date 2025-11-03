import { useState,useRef, useEffect } from 'react';
import classes from './EmojiPicker.module.scss';

const EMOJI_CATEGORIES = {
  '야구': ['⚾', '🏟️', '🥎', '🏆', '🎯', '🔥', '💪', '👏', '🙌', '🎉', '🎊', '💯', '⭐', '🌟', '💥', '🚀'],
  '얼굴': ['😀', '😃', '😄', '😁', '😆', '😅', '😂', '🤣', '😊', '😇', '🙂', '🙃', '😉', '😌', '😍', '🥰', '😘', '😗', '😙', '😚', '😋', '😛', '😝', '😜', '🤪', '🤨', '🧐', '🤓', '😎', '🤩', '🥳', '😏', '😒', '😞', '😔', '😟', '😕', '🙁', '☹️', '😣', '😖', '😫', '😩', '🥺', '😢', '😭', '😤', '😠', '😡', '🤬', '🤯', '😳', '🥵', '🥶', '😱', '😨', '😰', '😥', '😓'],
  '손짓': ['👍', '👎', '👌', '✌️', '🤞', '🤟', '🤘', '🤙', '👈', '👉', '👆', '🖕', '👇', '☝️', '✋', '🤚', '🖐️', '🖖', '👋', '🤝', '👏', '🙌', '👐', '🤲', '🤜', '🤛', '✊', '👊', '👎', '👍', '👌', '✌️', '🤞', '🤟', '🤘', '🤙', '👈', '👉', '👆', '🖕', '👇', '☝️', '✋', '🤚', '🖐️', '🖖', '👋', '🤝', '👏', '🙌', '👐', '🤲', '🤜', '🤛', '✊', '👊'],
  
  '기타': ['❤️', '🧡', '💛', '💚', '💙', '💜', '🖤', '🤍', '🤎', '💔', '❣️', '💕', '💞', '💓', '💗', '💖', '💘', '💝', '💟', '☮️', '✝️', '☪️', '🕉️', '☸️', '✡️', '🔯', '🕎', '☯️', '☦️', '🛐', '⛎', '♈', '♉', '♊', '♋', '♌', '♍', '♎', '♏', '♐', '♑', '♒', '♓', '🆔', '⚛️', '🉑', '☢️', '☣️', '📴', '📳', '🈶', '🈚', '🈸', '🈺', '🈷️', '✴️', '🆚', '💮', '🉐', '㊙️', '㊗️', '🈴', '🈵', '🈹', '🈲', '🅰️', '🅱️', '🆎', '🆑', '🅾️', '🆘', '❌', '⭕', '🛑', '⛔', '📛', '🚫', '💯', '💢', '♨️', '🚷', '🚯', '🚳', '🚱', '🔞', '📵', '🚭', '❗', '❕', '❓', '❔', '‼️', '⁉️', '🔅', '🔆', '〽️', '⚠️', '🚸', '🔱', '⚜️', '🔰', '♻️', '✅', '🈯', '💹', '❇️', '✳️', '❎', '🌐', '💠', 'Ⓜ️', '🌀', '💤', '🏧', '🚾', '♿', '🅿️', '🈳', '🈂️', '🛂', '🛃', '🛄', '🛅', '🚹', '🚺', '🚼', '🚻', '🚮', '🎦', '📶', '🈁', '🔣', 'ℹ️', '🔤', '🔡', '🔠', '🆖', '🆗', '🆙', '🆒', '🆕', '🆓', '0️⃣', '1️⃣', '2️⃣', '3️⃣', '4️⃣', '5️⃣', '6️⃣', '7️⃣', '8️⃣', '9️⃣', '🔟']
};

export default function EmojiPicker({ onEmojiSelect, isVisible, onClose }) {
    const [selectedCategory, setSelectedCategory] = useState('야구');
    const pickerRef = useRef(null);
    //외부 클릭 시 피커 닫기
    useEffect(() => {
        function handleClickOutside(event) {
            // 이모지 피커 내부 클릭은 무시
            if (pickerRef.current && 
            !pickerRef.current.contains(event.target) &&
            !event.target.closest('[data-emoji-picker]')) {
            onClose();
        }
    }
    if (isVisible) {
        document.addEventListener('mousedown', handleClickOutside);
    }
    return () => {
        document.removeEventListener('mousedown', handleClickOutside);
    };
    }, [isVisible, onClose]);
    
    const handleOverlayClick = (e) => {
        if (e.target === e.currentTarget) {
          onClose();
        }
    };
  
    
    const handleEmojiClick = (emoji) => {
        onEmojiSelect(emoji);
        
      
    };
    if (!isVisible) return null;

    return (
        <div className={classes.overlay} onClick={handleOverlayClick}>
            <div className={classes.emojiPicker} onClick={(e) => e.stopPropagation()}>
                {/* 카테고리 탭 */}
                <div className={classes.emojiCategories}>
                {Object.keys(EMOJI_CATEGORIES).map(category => (
                    <button 
                        key={category}
                        type="button"
                        onClick={(e) => {
                          e.preventDefault();
                          e.stopPropagation();
                          setSelectedCategory(category);
                        }}
                        className={`${classes.categoryButton} ${selectedCategory === category ? classes.active : ''}`}
                    >
                        {category}
                    </button>
                ))}
                </div>
                
                {/* 이모지 그리드 */}
                <div className={classes.emojiGrid}>
                    {EMOJI_CATEGORIES[selectedCategory].map((emoji, index) => (
                        <button 
                            key={`${emoji}-${index}`}
                            type="button"
                            onClick={(e) => {
                              e.preventDefault();
                              e.stopPropagation();
                              handleEmojiClick(emoji);
                            }}
                            className={classes.emojiItem}
                            title={emoji}
                        >
                          {emoji}
                        </button>
                    ))}
                </div>
            </div>
        </div>
    );
}
