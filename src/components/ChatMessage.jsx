import React from 'react';
import classes from "/pages/Chat.module.scss";
import { teamColors } from "/config/constants";
import TeamLogo from "./TeamLogo";


// 이모지 렌더링 함수
function renderMessageWithEmojis(message) {
    if (typeof message !== 'string') return message;
    
    // 이모지 유니코드 범위 체크
    const emojiRegex = /[\u{1F600}-\u{1F64F}]|[\u{1F300}-\u{1F5FF}]|[\u{1F680}-\u{1F6FF}]|[\u{1F1E0}-\u{1F1FF}]|[\u{2600}-\u{26FF}]|[\u{2700}-\u{27BF}]|[\u{1F900}-\u{1F9FF}]|[\u{1FA70}-\u{1FAFF}]|[\u{2190}-\u{21FF}]|[\u{2B00}-\u{2BFF}]|[\u{FE00}-\u{FE0F}]|[\u{1F000}-\u{1F02F}]|[\u{1F0A0}-\u{1F0FF}]|[\u{1F200}-\u{1F2FF}]|[\u{1F300}-\u{1F5FF}]|[\u{1F600}-\u{1F64F}]|[\u{1F680}-\u{1F6FF}]|[\u{1F700}-\u{1F77F}]|[\u{1F780}-\u{1F7FF}]|[\u{1F800}-\u{1F8FF}]|[\u{1F900}-\u{1F9FF}]|[\u{1FA00}-\u{1FA6F}]|[\u{1FA70}-\u{1FAFF}]|[\u{231A}-\u{231B}]|[\u{23E9}-\u{23EC}]|[\u{23F0}]|[\u{23F3}]|[\u{25FD}-\u{25FE}]|[\u{2614}-\u{2615}]|[\u{2648}-\u{2653}]|[\u{267F}]|[\u{2693}]|[\u{26A1}]|[\u{26AA}-\u{26AB}]|[\u{26BD}-\u{26BE}]|[\u{26C4}-\u{26C5}]|[\u{26CE}]|[\u{26D4}]|[\u{26EA}]|[\u{26F2}-\u{26F3}]|[\u{26F5}]|[\u{26FA}]|[\u{26FD}]|[\u{2705}]|[\u{270A}-\u{270B}]|[\u{2728}]|[\u{274C}]|[\u{274E}]|[\u{2753}-\u{2755}]|[\u{2757}]|[\u{2795}-\u{2797}]|[\u{27B0}]|[\u{27BF}]|[\u{2B1B}-\u{2B1C}]|[\u{2B50}]|[\u{2B55}]/gu;

    // 이모지만 있는지 체크
    const trimmedMessage = message.trim();
    const emojiMatches = trimmedMessage.match(emojiRegex);
    const nonEmojiText = trimmedMessage.replace(emojiRegex, '').trim();
    const emojiCount = emojiMatches ? emojiMatches.length : 0;
    const isOnlyEmoji = emojiCount > 0 && emojiCount <= 3 && nonEmojiText === '';
    
    // 이모지를 하나의 단위로 처리 (surrogate pair 문제 해결)
    const className = isOnlyEmoji ? 'emojiLarge' : 'emoji';
    const parts = [];
    let lastIndex = 0;
    let match;
    
    // 정규식을 재설정 (lastIndex 초기화)
    emojiRegex.lastIndex = 0;
    
    while ((match = emojiRegex.exec(message)) !== null) {
        // 이모지 이전의 일반 텍스트
        if (match.index > lastIndex) {
            parts.push({
                type: 'text',
                content: message.slice(lastIndex, match.index)
            });
        }
        // 이모지
        parts.push({
            type: 'emoji',
            content: match[0],
            className: className
        });
        lastIndex = emojiRegex.lastIndex;
    }
    
    // 마지막 남은 일반 텍스트
    if (lastIndex < message.length) {
        parts.push({
            type: 'text',
            content: message.slice(lastIndex)
        });
    }

    return parts.map((part, index) => {
        if (part.type === 'emoji') {
            console.log(`Emoji "${part.content}" rendered with class: ${part.className}`);
            return <span key={index} className={part.className}>{part.content}</span>;
        }
        return <span key={index}>{part.content}</span>;
    });
}
// JSX의 children을 이모지 처리하는 함수
function processJsxChildren(children) {
    return React.Children.map(children, (child) => {
        // 문자열인 경우 이모지 처리
        if (typeof child === 'string') {
            return renderMessageWithEmojis(child);
        }
        
        // React 요소인 경우 children을 재귀적으로 처리
        if (React.isValidElement(child)) {
            // className이 "pt-1 pb-1"인 div를 찾아서 메시지 처리
            if (child.type === 'div' && child.props.className && child.props.className.includes('pt-1 pb-1')) {
                const originalMessage = React.Children.toArray(child.props.children).join('');
                return React.cloneElement(child, {
                    children: renderMessageWithEmojis(originalMessage)
                });
            }
            
            // 다른 children도 재귀적으로 처리
            if (child.props.children) {
                return React.cloneElement(child, {
                    children: processJsxChildren(child.props.children)
                });
            }
        }
        
        return child;
    });
}


export default function ChatMessage({ type, message, team, audioUrl }) {
    // AI 봇, 편파해설, 클린봇은 이모지 처리 안함
    if (type === 'notice') return <div className={classes.notice}>{message}</div>;
    if (type === 'bias-comment') return <>
        <div className={classes['comment-wrapper']} style={{ minWidth: '100%', backgroundColor: teamColors[team.idKey], borderColor: teamColors[team.idKey] }}>
            <div className="d-flex align-items-center gap-8 bg-white border-radius-12 p-1 ps-2 pe-2">
                <TeamLogo name={team.idKey} zoom={0.7} />
                <b className="small" style={{ color: '#333' }}>{team.name} AI 편파해설</b>
                <img src="/assets/icons/chatbot.png" width="30px" className="fa-bounce" style={{ marginLeft: 'auto' }} />
            </div>
            <div className={classes['comment-message']}>{message}</div>
        </div>
    </>;
    if (type === 'aiBot') return <>
        <div className={classes['ai-bot-wrapper']}>
            <span className={classes['ai-bot-icon']}><img src="/assets/icons/chatbot.png" width="30px" /></span>
            <div className={classes['ai-bot-message']} dangerouslySetInnerHTML={{ __html: message }}></div>
        </div>
    </>;
    if (type === 'cleanBot') return <>
        <div className={classes['clean-bot-wrapper']}>
            <span className={classes['clean-bot-icon']}><img src="/assets/icons/chatbot.png" className="fa-shake fa-slow" width="30px" /></span>
            <div className={classes['clean-bot-message']}>{message}</div>
        </div>
    </>;
    
     // send/receive 타입: JSX를 받았을 때 이모지 처리
     if (type === 'send' || type === 'receive') {
        // message가 JSX인 경우
        if (React.isValidElement(message)) {
            return <div className={classes[type]}>
                <div className={classes.message}>{processJsxChildren([message])}</div>
                </div>;
        }
        
        // message가 문자열인 경우
        return <div className={classes[type]}>
            <div className={classes.message}>{renderMessageWithEmojis(message)}</div>
        </div>;
    }
    
    // 기타 타입 (fallback)
    return <div className={classes[type]}>
        <div className={classes.message}>{renderMessageWithEmojis(message)}</div>
    </div>;
}