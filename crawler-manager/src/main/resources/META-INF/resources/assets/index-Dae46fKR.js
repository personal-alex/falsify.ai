const __vite__mapDeps=(i,m=__vite__mapDeps,d=(m.f||(m.f=["assets/CrawlerDetail-CBjj3pSt.js","assets/primevue-b03PsPUA.js","assets/vendor-CQjc8Gll.js","assets/CrawlerDetail-BLpO3ZcR.css"])))=>i.map(i=>d[i]);
var Xe=Object.defineProperty;var qe=(e,t,n)=>t in e?Xe(e,t,{enumerable:!0,configurable:!0,writable:!0,value:n}):e[t]=n;var H=(e,t,n)=>qe(e,typeof t!="symbol"?t+"":t,n);import{x as _e,h as d,f as i,i as a,m as l,l as ae,d as x,v as I,e as y,F,t as w,y as Je,p as M,k as A,q as P,z as v,A as Qe,B as Ie,C as z,D as je,c as C,s as K,j as et,E as ie,G as pe,H as E,R as tt,I as nt,J as ot,r as O,o as st,K as rt,L as at,M as it,N as lt,O as ct,P as dt}from"./vendor-CQjc8Gll.js";import{s as De,B as Z,a as ne,b as Te,R as fe,c as be,d as ge,e as R,f as oe,K as Oe,g as ut,x as N,h as mt,t as Me,i as pt,j as B,F as ft,v as gt,C as ht,k as bt,S as yt,l as ye,W as vt,m as wt,P as Ct}from"./primevue-b03PsPUA.js";(function(){const t=document.createElement("link").relList;if(t&&t.supports&&t.supports("modulepreload"))return;for(const r of document.querySelectorAll('link[rel="modulepreload"]'))s(r);new MutationObserver(r=>{for(const o of r)if(o.type==="childList")for(const c of o.addedNodes)c.tagName==="LINK"&&c.rel==="modulepreload"&&s(c)}).observe(document,{childList:!0,subtree:!0});function n(r){const o={};return r.integrity&&(o.integrity=r.integrity),r.referrerPolicy&&(o.referrerPolicy=r.referrerPolicy),r.crossOrigin==="use-credentials"?o.credentials="include":r.crossOrigin==="anonymous"?o.credentials="omit":o.credentials="same-origin",o}function s(r){if(r.ep)return;r.ep=!0;const o=n(r);fetch(r.href,o)}})();var L=De(),Be=Symbol();function Ae(){var e=_e(Be);if(!e)throw new Error("No PrimeVue Toast provided!");return e}var kt={install:function(t){var n={add:function(r){L.emit("add",r)},remove:function(r){L.emit("remove",r)},removeGroup:function(r){L.emit("remove-group",r)},removeAllGroups:function(){L.emit("remove-all-groups")}};t.config.globalProperties.$toast=n,t.provide(Be,n)}},V=De(),xt=Symbol(),St={install:function(t){var n={require:function(r){V.emit("confirm",r)},close:function(){V.emit("close")}};t.config.globalProperties.$confirm=n,t.provide(xt,n)}},Pt=`
    .p-toast {
        width: dt('toast.width');
        white-space: pre-line;
        word-break: break-word;
    }

    .p-toast-message {
        margin: 0 0 1rem 0;
    }

    .p-toast-message-icon {
        flex-shrink: 0;
        font-size: dt('toast.icon.size');
        width: dt('toast.icon.size');
        height: dt('toast.icon.size');
    }

    .p-toast-message-content {
        display: flex;
        align-items: flex-start;
        padding: dt('toast.content.padding');
        gap: dt('toast.content.gap');
    }

    .p-toast-message-text {
        flex: 1 1 auto;
        display: flex;
        flex-direction: column;
        gap: dt('toast.text.gap');
    }

    .p-toast-summary {
        font-weight: dt('toast.summary.font.weight');
        font-size: dt('toast.summary.font.size');
    }

    .p-toast-detail {
        font-weight: dt('toast.detail.font.weight');
        font-size: dt('toast.detail.font.size');
    }

    .p-toast-close-button {
        display: flex;
        align-items: center;
        justify-content: center;
        overflow: hidden;
        position: relative;
        cursor: pointer;
        background: transparent;
        transition:
            background dt('toast.transition.duration'),
            color dt('toast.transition.duration'),
            outline-color dt('toast.transition.duration'),
            box-shadow dt('toast.transition.duration');
        outline-color: transparent;
        color: inherit;
        width: dt('toast.close.button.width');
        height: dt('toast.close.button.height');
        border-radius: dt('toast.close.button.border.radius');
        margin: -25% 0 0 0;
        right: -25%;
        padding: 0;
        border: none;
        user-select: none;
    }

    .p-toast-close-button:dir(rtl) {
        margin: -25% 0 0 auto;
        left: -25%;
        right: auto;
    }

    .p-toast-message-info,
    .p-toast-message-success,
    .p-toast-message-warn,
    .p-toast-message-error,
    .p-toast-message-secondary,
    .p-toast-message-contrast {
        border-width: dt('toast.border.width');
        border-style: solid;
        backdrop-filter: blur(dt('toast.blur'));
        border-radius: dt('toast.border.radius');
    }

    .p-toast-close-icon {
        font-size: dt('toast.close.icon.size');
        width: dt('toast.close.icon.size');
        height: dt('toast.close.icon.size');
    }

    .p-toast-close-button:focus-visible {
        outline-width: dt('focus.ring.width');
        outline-style: dt('focus.ring.style');
        outline-offset: dt('focus.ring.offset');
    }

    .p-toast-message-info {
        background: dt('toast.info.background');
        border-color: dt('toast.info.border.color');
        color: dt('toast.info.color');
        box-shadow: dt('toast.info.shadow');
    }

    .p-toast-message-info .p-toast-detail {
        color: dt('toast.info.detail.color');
    }

    .p-toast-message-info .p-toast-close-button:focus-visible {
        outline-color: dt('toast.info.close.button.focus.ring.color');
        box-shadow: dt('toast.info.close.button.focus.ring.shadow');
    }

    .p-toast-message-info .p-toast-close-button:hover {
        background: dt('toast.info.close.button.hover.background');
    }

    .p-toast-message-success {
        background: dt('toast.success.background');
        border-color: dt('toast.success.border.color');
        color: dt('toast.success.color');
        box-shadow: dt('toast.success.shadow');
    }

    .p-toast-message-success .p-toast-detail {
        color: dt('toast.success.detail.color');
    }

    .p-toast-message-success .p-toast-close-button:focus-visible {
        outline-color: dt('toast.success.close.button.focus.ring.color');
        box-shadow: dt('toast.success.close.button.focus.ring.shadow');
    }

    .p-toast-message-success .p-toast-close-button:hover {
        background: dt('toast.success.close.button.hover.background');
    }

    .p-toast-message-warn {
        background: dt('toast.warn.background');
        border-color: dt('toast.warn.border.color');
        color: dt('toast.warn.color');
        box-shadow: dt('toast.warn.shadow');
    }

    .p-toast-message-warn .p-toast-detail {
        color: dt('toast.warn.detail.color');
    }

    .p-toast-message-warn .p-toast-close-button:focus-visible {
        outline-color: dt('toast.warn.close.button.focus.ring.color');
        box-shadow: dt('toast.warn.close.button.focus.ring.shadow');
    }

    .p-toast-message-warn .p-toast-close-button:hover {
        background: dt('toast.warn.close.button.hover.background');
    }

    .p-toast-message-error {
        background: dt('toast.error.background');
        border-color: dt('toast.error.border.color');
        color: dt('toast.error.color');
        box-shadow: dt('toast.error.shadow');
    }

    .p-toast-message-error .p-toast-detail {
        color: dt('toast.error.detail.color');
    }

    .p-toast-message-error .p-toast-close-button:focus-visible {
        outline-color: dt('toast.error.close.button.focus.ring.color');
        box-shadow: dt('toast.error.close.button.focus.ring.shadow');
    }

    .p-toast-message-error .p-toast-close-button:hover {
        background: dt('toast.error.close.button.hover.background');
    }

    .p-toast-message-secondary {
        background: dt('toast.secondary.background');
        border-color: dt('toast.secondary.border.color');
        color: dt('toast.secondary.color');
        box-shadow: dt('toast.secondary.shadow');
    }

    .p-toast-message-secondary .p-toast-detail {
        color: dt('toast.secondary.detail.color');
    }

    .p-toast-message-secondary .p-toast-close-button:focus-visible {
        outline-color: dt('toast.secondary.close.button.focus.ring.color');
        box-shadow: dt('toast.secondary.close.button.focus.ring.shadow');
    }

    .p-toast-message-secondary .p-toast-close-button:hover {
        background: dt('toast.secondary.close.button.hover.background');
    }

    .p-toast-message-contrast {
        background: dt('toast.contrast.background');
        border-color: dt('toast.contrast.border.color');
        color: dt('toast.contrast.color');
        box-shadow: dt('toast.contrast.shadow');
    }

    .p-toast-message-contrast .p-toast-detail {
        color: dt('toast.contrast.detail.color');
    }

    .p-toast-message-contrast .p-toast-close-button:focus-visible {
        outline-color: dt('toast.contrast.close.button.focus.ring.color');
        box-shadow: dt('toast.contrast.close.button.focus.ring.shadow');
    }

    .p-toast-message-contrast .p-toast-close-button:hover {
        background: dt('toast.contrast.close.button.hover.background');
    }

    .p-toast-top-center {
        transform: translateX(-50%);
    }

    .p-toast-bottom-center {
        transform: translateX(-50%);
    }

    .p-toast-center {
        min-width: 20vw;
        transform: translate(-50%, -50%);
    }

    .p-toast-message-enter-from {
        opacity: 0;
        transform: translateY(50%);
    }

    .p-toast-message-leave-from {
        max-height: 1000px;
    }

    .p-toast .p-toast-message.p-toast-message-leave-to {
        max-height: 0;
        opacity: 0;
        margin-bottom: 0;
        overflow: hidden;
    }

    .p-toast-message-enter-active {
        transition:
            transform 0.3s,
            opacity 0.3s;
    }

    .p-toast-message-leave-active {
        transition:
            max-height 0.45s cubic-bezier(0, 1, 0, 1),
            opacity 0.3s,
            margin-bottom 0.3s;
    }
`;function G(e){"@babel/helpers - typeof";return G=typeof Symbol=="function"&&typeof Symbol.iterator=="symbol"?function(t){return typeof t}:function(t){return t&&typeof Symbol=="function"&&t.constructor===Symbol&&t!==Symbol.prototype?"symbol":typeof t},G(e)}function re(e,t,n){return(t=$t(t))in e?Object.defineProperty(e,t,{value:n,enumerable:!0,configurable:!0,writable:!0}):e[t]=n,e}function $t(e){var t=Lt(e,"string");return G(t)=="symbol"?t:t+""}function Lt(e,t){if(G(e)!="object"||!e)return e;var n=e[Symbol.toPrimitive];if(n!==void 0){var s=n.call(e,t);if(G(s)!="object")return s;throw new TypeError("@@toPrimitive must return a primitive value.")}return(t==="string"?String:Number)(e)}var Et={root:function(t){var n=t.position;return{position:"fixed",top:n==="top-right"||n==="top-left"||n==="top-center"?"20px":n==="center"?"50%":null,right:(n==="top-right"||n==="bottom-right")&&"20px",bottom:(n==="bottom-left"||n==="bottom-right"||n==="bottom-center")&&"20px",left:n==="top-left"||n==="bottom-left"?"20px":n==="center"||n==="top-center"||n==="bottom-center"?"50%":null}}},zt={root:function(t){var n=t.props;return["p-toast p-component p-toast-"+n.position]},message:function(t){var n=t.props;return["p-toast-message",{"p-toast-message-info":n.message.severity==="info"||n.message.severity===void 0,"p-toast-message-warn":n.message.severity==="warn","p-toast-message-error":n.message.severity==="error","p-toast-message-success":n.message.severity==="success","p-toast-message-secondary":n.message.severity==="secondary","p-toast-message-contrast":n.message.severity==="contrast"}]},messageContent:"p-toast-message-content",messageIcon:function(t){var n=t.props;return["p-toast-message-icon",re(re(re(re({},n.infoIcon,n.message.severity==="info"),n.warnIcon,n.message.severity==="warn"),n.errorIcon,n.message.severity==="error"),n.successIcon,n.message.severity==="success")]},messageText:"p-toast-message-text",summary:"p-toast-summary",detail:"p-toast-detail",closeButton:"p-toast-close-button",closeIcon:"p-toast-close-icon"},It=Z.extend({name:"toast",style:Pt,classes:zt,inlineStyles:Et}),le={name:"ExclamationTriangleIcon",extends:ne};function jt(e,t,n,s,r,o){return i(),d("svg",l({width:"14",height:"14",viewBox:"0 0 14 14",fill:"none",xmlns:"http://www.w3.org/2000/svg"},e.pti()),t[0]||(t[0]=[a("path",{d:"M13.4018 13.1893H0.598161C0.49329 13.189 0.390283 13.1615 0.299143 13.1097C0.208003 13.0578 0.131826 12.9832 0.0780112 12.8932C0.0268539 12.8015 0 12.6982 0 12.5931C0 12.4881 0.0268539 12.3848 0.0780112 12.293L6.47985 1.08982C6.53679 1.00399 6.61408 0.933574 6.70484 0.884867C6.7956 0.836159 6.897 0.810669 7 0.810669C7.103 0.810669 7.2044 0.836159 7.29516 0.884867C7.38592 0.933574 7.46321 1.00399 7.52015 1.08982L13.922 12.293C13.9731 12.3848 14 12.4881 14 12.5931C14 12.6982 13.9731 12.8015 13.922 12.8932C13.8682 12.9832 13.792 13.0578 13.7009 13.1097C13.6097 13.1615 13.5067 13.189 13.4018 13.1893ZM1.63046 11.989H12.3695L7 2.59425L1.63046 11.989Z",fill:"currentColor"},null,-1),a("path",{d:"M6.99996 8.78801C6.84143 8.78594 6.68997 8.72204 6.57787 8.60993C6.46576 8.49782 6.40186 8.34637 6.39979 8.18784V5.38703C6.39979 5.22786 6.46302 5.0752 6.57557 4.96265C6.68813 4.85009 6.84078 4.78686 6.99996 4.78686C7.15914 4.78686 7.31179 4.85009 7.42435 4.96265C7.5369 5.0752 7.60013 5.22786 7.60013 5.38703V8.18784C7.59806 8.34637 7.53416 8.49782 7.42205 8.60993C7.30995 8.72204 7.15849 8.78594 6.99996 8.78801Z",fill:"currentColor"},null,-1),a("path",{d:"M6.99996 11.1887C6.84143 11.1866 6.68997 11.1227 6.57787 11.0106C6.46576 10.8985 6.40186 10.7471 6.39979 10.5885V10.1884C6.39979 10.0292 6.46302 9.87658 6.57557 9.76403C6.68813 9.65147 6.84078 9.58824 6.99996 9.58824C7.15914 9.58824 7.31179 9.65147 7.42435 9.76403C7.5369 9.87658 7.60013 10.0292 7.60013 10.1884V10.5885C7.59806 10.7471 7.53416 10.8985 7.42205 11.0106C7.30995 11.1227 7.15849 11.1866 6.99996 11.1887Z",fill:"currentColor"},null,-1)]),16)}le.render=jt;var ce={name:"InfoCircleIcon",extends:ne};function Dt(e,t,n,s,r,o){return i(),d("svg",l({width:"14",height:"14",viewBox:"0 0 14 14",fill:"none",xmlns:"http://www.w3.org/2000/svg"},e.pti()),t[0]||(t[0]=[a("path",{"fill-rule":"evenodd","clip-rule":"evenodd",d:"M3.11101 12.8203C4.26215 13.5895 5.61553 14 7 14C8.85652 14 10.637 13.2625 11.9497 11.9497C13.2625 10.637 14 8.85652 14 7C14 5.61553 13.5895 4.26215 12.8203 3.11101C12.0511 1.95987 10.9579 1.06266 9.67879 0.532846C8.3997 0.00303296 6.99224 -0.13559 5.63437 0.134506C4.2765 0.404603 3.02922 1.07129 2.05026 2.05026C1.07129 3.02922 0.404603 4.2765 0.134506 5.63437C-0.13559 6.99224 0.00303296 8.3997 0.532846 9.67879C1.06266 10.9579 1.95987 12.0511 3.11101 12.8203ZM3.75918 2.14976C4.71846 1.50879 5.84628 1.16667 7 1.16667C8.5471 1.16667 10.0308 1.78125 11.1248 2.87521C12.2188 3.96918 12.8333 5.45291 12.8333 7C12.8333 8.15373 12.4912 9.28154 11.8502 10.2408C11.2093 11.2001 10.2982 11.9478 9.23232 12.3893C8.16642 12.8308 6.99353 12.9463 5.86198 12.7212C4.73042 12.4962 3.69102 11.9406 2.87521 11.1248C2.05941 10.309 1.50384 9.26958 1.27876 8.13803C1.05367 7.00647 1.16919 5.83358 1.61071 4.76768C2.05222 3.70178 2.79989 2.79074 3.75918 2.14976ZM7.00002 4.8611C6.84594 4.85908 6.69873 4.79698 6.58977 4.68801C6.48081 4.57905 6.4187 4.43185 6.41669 4.27776V3.88888C6.41669 3.73417 6.47815 3.58579 6.58754 3.4764C6.69694 3.367 6.84531 3.30554 7.00002 3.30554C7.15473 3.30554 7.3031 3.367 7.4125 3.4764C7.52189 3.58579 7.58335 3.73417 7.58335 3.88888V4.27776C7.58134 4.43185 7.51923 4.57905 7.41027 4.68801C7.30131 4.79698 7.1541 4.85908 7.00002 4.8611ZM7.00002 10.6945C6.84594 10.6925 6.69873 10.6304 6.58977 10.5214C6.48081 10.4124 6.4187 10.2652 6.41669 10.1111V6.22225C6.41669 6.06754 6.47815 5.91917 6.58754 5.80977C6.69694 5.70037 6.84531 5.63892 7.00002 5.63892C7.15473 5.63892 7.3031 5.70037 7.4125 5.80977C7.52189 5.91917 7.58335 6.06754 7.58335 6.22225V10.1111C7.58134 10.2652 7.51923 10.4124 7.41027 10.5214C7.30131 10.6304 7.1541 10.6925 7.00002 10.6945Z",fill:"currentColor"},null,-1)]),16)}ce.render=Dt;var de={name:"TimesCircleIcon",extends:ne};function Tt(e,t,n,s,r,o){return i(),d("svg",l({width:"14",height:"14",viewBox:"0 0 14 14",fill:"none",xmlns:"http://www.w3.org/2000/svg"},e.pti()),t[0]||(t[0]=[a("path",{"fill-rule":"evenodd","clip-rule":"evenodd",d:"M7 14C5.61553 14 4.26215 13.5895 3.11101 12.8203C1.95987 12.0511 1.06266 10.9579 0.532846 9.67879C0.00303296 8.3997 -0.13559 6.99224 0.134506 5.63437C0.404603 4.2765 1.07129 3.02922 2.05026 2.05026C3.02922 1.07129 4.2765 0.404603 5.63437 0.134506C6.99224 -0.13559 8.3997 0.00303296 9.67879 0.532846C10.9579 1.06266 12.0511 1.95987 12.8203 3.11101C13.5895 4.26215 14 5.61553 14 7C14 8.85652 13.2625 10.637 11.9497 11.9497C10.637 13.2625 8.85652 14 7 14ZM7 1.16667C5.84628 1.16667 4.71846 1.50879 3.75918 2.14976C2.79989 2.79074 2.05222 3.70178 1.61071 4.76768C1.16919 5.83358 1.05367 7.00647 1.27876 8.13803C1.50384 9.26958 2.05941 10.309 2.87521 11.1248C3.69102 11.9406 4.73042 12.4962 5.86198 12.7212C6.99353 12.9463 8.16642 12.8308 9.23232 12.3893C10.2982 11.9478 11.2093 11.2001 11.8502 10.2408C12.4912 9.28154 12.8333 8.15373 12.8333 7C12.8333 5.45291 12.2188 3.96918 11.1248 2.87521C10.0308 1.78125 8.5471 1.16667 7 1.16667ZM4.66662 9.91668C4.58998 9.91704 4.51404 9.90209 4.44325 9.87271C4.37246 9.84333 4.30826 9.8001 4.2544 9.74557C4.14516 9.6362 4.0838 9.48793 4.0838 9.33335C4.0838 9.17876 4.14516 9.0305 4.2544 8.92113L6.17553 7L4.25443 5.07891C4.15139 4.96832 4.09529 4.82207 4.09796 4.67094C4.10063 4.51982 4.16185 4.37563 4.26872 4.26876C4.3756 4.16188 4.51979 4.10066 4.67091 4.09799C4.82204 4.09532 4.96829 4.15142 5.07887 4.25446L6.99997 6.17556L8.92106 4.25446C9.03164 4.15142 9.1779 4.09532 9.32903 4.09799C9.48015 4.10066 9.62434 4.16188 9.73121 4.26876C9.83809 4.37563 9.89931 4.51982 9.90198 4.67094C9.90464 4.82207 9.84855 4.96832 9.74551 5.07891L7.82441 7L9.74554 8.92113C9.85478 9.0305 9.91614 9.17876 9.91614 9.33335C9.91614 9.48793 9.85478 9.6362 9.74554 9.74557C9.69168 9.8001 9.62748 9.84333 9.55669 9.87271C9.4859 9.90209 9.40996 9.91704 9.33332 9.91668C9.25668 9.91704 9.18073 9.90209 9.10995 9.87271C9.03916 9.84333 8.97495 9.8001 8.9211 9.74557L6.99997 7.82444L5.07884 9.74557C5.02499 9.8001 4.96078 9.84333 4.88999 9.87271C4.81921 9.90209 4.74326 9.91704 4.66662 9.91668Z",fill:"currentColor"},null,-1)]),16)}de.render=Tt;var Ot={name:"BaseToast",extends:R,props:{group:{type:String,default:null},position:{type:String,default:"top-right"},autoZIndex:{type:Boolean,default:!0},baseZIndex:{type:Number,default:0},breakpoints:{type:Object,default:null},closeIcon:{type:String,default:void 0},infoIcon:{type:String,default:void 0},warnIcon:{type:String,default:void 0},errorIcon:{type:String,default:void 0},successIcon:{type:String,default:void 0},closeButtonProps:{type:null,default:null},onMouseEnter:{type:Function,default:void 0},onMouseLeave:{type:Function,default:void 0},onClick:{type:Function,default:void 0}},style:It,provide:function(){return{$pcToast:this,$parentInstance:this}}};function Y(e){"@babel/helpers - typeof";return Y=typeof Symbol=="function"&&typeof Symbol.iterator=="symbol"?function(t){return typeof t}:function(t){return t&&typeof Symbol=="function"&&t.constructor===Symbol&&t!==Symbol.prototype?"symbol":typeof t},Y(e)}function Mt(e,t,n){return(t=Bt(t))in e?Object.defineProperty(e,t,{value:n,enumerable:!0,configurable:!0,writable:!0}):e[t]=n,e}function Bt(e){var t=At(e,"string");return Y(t)=="symbol"?t:t+""}function At(e,t){if(Y(e)!="object"||!e)return e;var n=e[Symbol.toPrimitive];if(n!==void 0){var s=n.call(e,t);if(Y(s)!="object")return s;throw new TypeError("@@toPrimitive must return a primitive value.")}return(t==="string"?String:Number)(e)}var Re={name:"ToastMessage",hostName:"Toast",extends:R,emits:["close"],closeTimeout:null,createdAt:null,lifeRemaining:null,props:{message:{type:null,default:null},templates:{type:Object,default:null},closeIcon:{type:String,default:null},infoIcon:{type:String,default:null},warnIcon:{type:String,default:null},errorIcon:{type:String,default:null},successIcon:{type:String,default:null},closeButtonProps:{type:null,default:null},onMouseEnter:{type:Function,default:void 0},onMouseLeave:{type:Function,default:void 0},onClick:{type:Function,default:void 0}},mounted:function(){this.message.life&&(this.lifeRemaining=this.message.life,this.startTimeout())},beforeUnmount:function(){this.clearCloseTimeout()},methods:{startTimeout:function(){var t=this;this.createdAt=new Date().valueOf(),this.closeTimeout=setTimeout(function(){t.close({message:t.message,type:"life-end"})},this.lifeRemaining)},close:function(t){this.$emit("close",t)},onCloseClick:function(){this.clearCloseTimeout(),this.close({message:this.message,type:"close"})},clearCloseTimeout:function(){this.closeTimeout&&(clearTimeout(this.closeTimeout),this.closeTimeout=null)},onMessageClick:function(t){var n;(n=this.onClick)===null||n===void 0||n.call(this,{originalEvent:t,message:this.message})},handleMouseEnter:function(t){if(this.onMouseEnter){if(this.onMouseEnter({originalEvent:t,message:this.message}),t.defaultPrevented)return;this.message.life&&(this.lifeRemaining=this.createdAt+this.lifeRemaining-new Date().valueOf(),this.createdAt=null,this.clearCloseTimeout())}},handleMouseLeave:function(t){if(this.onMouseLeave){if(this.onMouseLeave({originalEvent:t,message:this.message}),t.defaultPrevented)return;this.message.life&&this.startTimeout()}}},computed:{iconComponent:function(){return{info:!this.infoIcon&&ce,success:!this.successIcon&&be,warn:!this.warnIcon&&le,error:!this.errorIcon&&de}[this.message.severity]},closeAriaLabel:function(){return this.$primevue.config.locale.aria?this.$primevue.config.locale.aria.close:void 0},dataP:function(){return oe(Mt({},this.message.severity,this.message.severity))}},components:{TimesIcon:ge,InfoCircleIcon:ce,CheckIcon:be,ExclamationTriangleIcon:le,TimesCircleIcon:de},directives:{ripple:fe}};function X(e){"@babel/helpers - typeof";return X=typeof Symbol=="function"&&typeof Symbol.iterator=="symbol"?function(t){return typeof t}:function(t){return t&&typeof Symbol=="function"&&t.constructor===Symbol&&t!==Symbol.prototype?"symbol":typeof t},X(e)}function ve(e,t){var n=Object.keys(e);if(Object.getOwnPropertySymbols){var s=Object.getOwnPropertySymbols(e);t&&(s=s.filter(function(r){return Object.getOwnPropertyDescriptor(e,r).enumerable})),n.push.apply(n,s)}return n}function we(e){for(var t=1;t<arguments.length;t++){var n=arguments[t]!=null?arguments[t]:{};t%2?ve(Object(n),!0).forEach(function(s){Rt(e,s,n[s])}):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(n)):ve(Object(n)).forEach(function(s){Object.defineProperty(e,s,Object.getOwnPropertyDescriptor(n,s))})}return e}function Rt(e,t,n){return(t=Ht(t))in e?Object.defineProperty(e,t,{value:n,enumerable:!0,configurable:!0,writable:!0}):e[t]=n,e}function Ht(e){var t=Ut(e,"string");return X(t)=="symbol"?t:t+""}function Ut(e,t){if(X(e)!="object"||!e)return e;var n=e[Symbol.toPrimitive];if(n!==void 0){var s=n.call(e,t);if(X(s)!="object")return s;throw new TypeError("@@toPrimitive must return a primitive value.")}return(t==="string"?String:Number)(e)}var Vt=["data-p"],Nt=["data-p"],Ft=["data-p"],Kt=["data-p"],Zt=["aria-label","data-p"];function Wt(e,t,n,s,r,o){var c=ae("ripple");return i(),d("div",l({class:[e.cx("message"),n.message.styleClass],role:"alert","aria-live":"assertive","aria-atomic":"true","data-p":o.dataP},e.ptm("message"),{onClick:t[1]||(t[1]=function(){return o.onMessageClick&&o.onMessageClick.apply(o,arguments)}),onMouseenter:t[2]||(t[2]=function(){return o.handleMouseEnter&&o.handleMouseEnter.apply(o,arguments)}),onMouseleave:t[3]||(t[3]=function(){return o.handleMouseLeave&&o.handleMouseLeave.apply(o,arguments)})}),[n.templates.container?(i(),x(I(n.templates.container),{key:0,message:n.message,closeCallback:o.onCloseClick},null,8,["message","closeCallback"])):(i(),d("div",l({key:1,class:[e.cx("messageContent"),n.message.contentStyleClass]},e.ptm("messageContent")),[n.templates.message?(i(),x(I(n.templates.message),{key:1,message:n.message},null,8,["message"])):(i(),d(F,{key:0},[(i(),x(I(n.templates.messageicon?n.templates.messageicon:n.templates.icon?n.templates.icon:o.iconComponent&&o.iconComponent.name?o.iconComponent:"span"),l({class:e.cx("messageIcon")},e.ptm("messageIcon")),null,16,["class"])),a("div",l({class:e.cx("messageText"),"data-p":o.dataP},e.ptm("messageText")),[a("span",l({class:e.cx("summary"),"data-p":o.dataP},e.ptm("summary")),w(n.message.summary),17,Ft),n.message.detail?(i(),d("div",l({key:0,class:e.cx("detail"),"data-p":o.dataP},e.ptm("detail")),w(n.message.detail),17,Kt)):y("",!0)],16,Nt)],64)),n.message.closable!==!1?(i(),d("div",Je(l({key:2},e.ptm("buttonContainer"))),[M((i(),d("button",l({class:e.cx("closeButton"),type:"button","aria-label":o.closeAriaLabel,onClick:t[0]||(t[0]=function(){return o.onCloseClick&&o.onCloseClick.apply(o,arguments)}),autofocus:"","data-p":o.dataP},we(we({},n.closeButtonProps),e.ptm("closeButton"))),[(i(),x(I(n.templates.closeicon||"TimesIcon"),l({class:[e.cx("closeIcon"),n.closeIcon]},e.ptm("closeIcon")),null,16,["class"]))],16,Zt)),[[c]])],16)):y("",!0)],16))],16,Vt)}Re.render=Wt;function q(e){"@babel/helpers - typeof";return q=typeof Symbol=="function"&&typeof Symbol.iterator=="symbol"?function(t){return typeof t}:function(t){return t&&typeof Symbol=="function"&&t.constructor===Symbol&&t!==Symbol.prototype?"symbol":typeof t},q(e)}function Gt(e,t,n){return(t=Yt(t))in e?Object.defineProperty(e,t,{value:n,enumerable:!0,configurable:!0,writable:!0}):e[t]=n,e}function Yt(e){var t=Xt(e,"string");return q(t)=="symbol"?t:t+""}function Xt(e,t){if(q(e)!="object"||!e)return e;var n=e[Symbol.toPrimitive];if(n!==void 0){var s=n.call(e,t);if(q(s)!="object")return s;throw new TypeError("@@toPrimitive must return a primitive value.")}return(t==="string"?String:Number)(e)}function qt(e){return en(e)||Qt(e)||Jt(e)||_t()}function _t(){throw new TypeError(`Invalid attempt to spread non-iterable instance.
In order to be iterable, non-array objects must have a [Symbol.iterator]() method.`)}function Jt(e,t){if(e){if(typeof e=="string")return ue(e,t);var n={}.toString.call(e).slice(8,-1);return n==="Object"&&e.constructor&&(n=e.constructor.name),n==="Map"||n==="Set"?Array.from(e):n==="Arguments"||/^(?:Ui|I)nt(?:8|16|32)(?:Clamped)?Array$/.test(n)?ue(e,t):void 0}}function Qt(e){if(typeof Symbol<"u"&&e[Symbol.iterator]!=null||e["@@iterator"]!=null)return Array.from(e)}function en(e){if(Array.isArray(e))return ue(e)}function ue(e,t){(t==null||t>e.length)&&(t=e.length);for(var n=0,s=Array(t);n<t;n++)s[n]=e[n];return s}var tn=0,He={name:"Toast",extends:Ot,inheritAttrs:!1,emits:["close","life-end"],data:function(){return{messages:[]}},styleElement:null,mounted:function(){L.on("add",this.onAdd),L.on("remove",this.onRemove),L.on("remove-group",this.onRemoveGroup),L.on("remove-all-groups",this.onRemoveAllGroups),this.breakpoints&&this.createStyle()},beforeUnmount:function(){this.destroyStyle(),this.$refs.container&&this.autoZIndex&&N.clear(this.$refs.container),L.off("add",this.onAdd),L.off("remove",this.onRemove),L.off("remove-group",this.onRemoveGroup),L.off("remove-all-groups",this.onRemoveAllGroups)},methods:{add:function(t){t.id==null&&(t.id=tn++),this.messages=[].concat(qt(this.messages),[t])},remove:function(t){var n=this.messages.findIndex(function(s){return s.id===t.message.id});n!==-1&&(this.messages.splice(n,1),this.$emit(t.type,{message:t.message}))},onAdd:function(t){this.group==t.group&&this.add(t)},onRemove:function(t){this.remove({message:t,type:"close"})},onRemoveGroup:function(t){this.group===t&&(this.messages=[])},onRemoveAllGroups:function(){var t=this;this.messages.forEach(function(n){return t.$emit("close",{message:n})}),this.messages=[]},onEnter:function(){this.autoZIndex&&N.set("modal",this.$refs.container,this.baseZIndex||this.$primevue.config.zIndex.modal)},onLeave:function(){var t=this;this.$refs.container&&this.autoZIndex&&ut(this.messages)&&setTimeout(function(){N.clear(t.$refs.container)},200)},createStyle:function(){if(!this.styleElement&&!this.isUnstyled){var t;this.styleElement=document.createElement("style"),this.styleElement.type="text/css",Oe(this.styleElement,"nonce",(t=this.$primevue)===null||t===void 0||(t=t.config)===null||t===void 0||(t=t.csp)===null||t===void 0?void 0:t.nonce),document.head.appendChild(this.styleElement);var n="";for(var s in this.breakpoints){var r="";for(var o in this.breakpoints[s])r+=o+":"+this.breakpoints[s][o]+"!important;";n+=`
                        @media screen and (max-width: `.concat(s,`) {
                            .p-toast[`).concat(this.$attrSelector,`] {
                                `).concat(r,`
                            }
                        }
                    `)}this.styleElement.innerHTML=n}},destroyStyle:function(){this.styleElement&&(document.head.removeChild(this.styleElement),this.styleElement=null)}},computed:{dataP:function(){return oe(Gt({},this.position,this.position))}},components:{ToastMessage:Re,Portal:Te}};function _(e){"@babel/helpers - typeof";return _=typeof Symbol=="function"&&typeof Symbol.iterator=="symbol"?function(t){return typeof t}:function(t){return t&&typeof Symbol=="function"&&t.constructor===Symbol&&t!==Symbol.prototype?"symbol":typeof t},_(e)}function Ce(e,t){var n=Object.keys(e);if(Object.getOwnPropertySymbols){var s=Object.getOwnPropertySymbols(e);t&&(s=s.filter(function(r){return Object.getOwnPropertyDescriptor(e,r).enumerable})),n.push.apply(n,s)}return n}function nn(e){for(var t=1;t<arguments.length;t++){var n=arguments[t]!=null?arguments[t]:{};t%2?Ce(Object(n),!0).forEach(function(s){on(e,s,n[s])}):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(n)):Ce(Object(n)).forEach(function(s){Object.defineProperty(e,s,Object.getOwnPropertyDescriptor(n,s))})}return e}function on(e,t,n){return(t=sn(t))in e?Object.defineProperty(e,t,{value:n,enumerable:!0,configurable:!0,writable:!0}):e[t]=n,e}function sn(e){var t=rn(e,"string");return _(t)=="symbol"?t:t+""}function rn(e,t){if(_(e)!="object"||!e)return e;var n=e[Symbol.toPrimitive];if(n!==void 0){var s=n.call(e,t);if(_(s)!="object")return s;throw new TypeError("@@toPrimitive must return a primitive value.")}return(t==="string"?String:Number)(e)}var an=["data-p"];function ln(e,t,n,s,r,o){var c=A("ToastMessage"),f=A("Portal");return i(),x(f,null,{default:P(function(){return[a("div",l({ref:"container",class:e.cx("root"),style:e.sx("root",!0,{position:e.position}),"data-p":o.dataP},e.ptmi("root")),[v(Qe,l({name:"p-toast-message",tag:"div",onEnter:o.onEnter,onLeave:o.onLeave},nn({},e.ptm("transition"))),{default:P(function(){return[(i(!0),d(F,null,Ie(r.messages,function(u){return i(),x(c,{key:u.id,message:u,templates:e.$slots,closeIcon:e.closeIcon,infoIcon:e.infoIcon,warnIcon:e.warnIcon,errorIcon:e.errorIcon,successIcon:e.successIcon,closeButtonProps:e.closeButtonProps,onMouseEnter:e.onMouseEnter,onMouseLeave:e.onMouseLeave,onClick:e.onClick,unstyled:e.unstyled,onClose:t[0]||(t[0]=function(p){return o.remove(p)}),pt:e.pt},null,8,["message","templates","closeIcon","infoIcon","warnIcon","errorIcon","successIcon","closeButtonProps","onMouseEnter","onMouseLeave","onClick","unstyled","pt"])}),128))]}),_:1},16,["onEnter","onLeave"])],16,an)]}),_:1})}He.render=ln;var Ue={name:"WindowMaximizeIcon",extends:ne};function cn(e,t,n,s,r,o){return i(),d("svg",l({width:"14",height:"14",viewBox:"0 0 14 14",fill:"none",xmlns:"http://www.w3.org/2000/svg"},e.pti()),t[0]||(t[0]=[a("path",{"fill-rule":"evenodd","clip-rule":"evenodd",d:"M7 14H11.8C12.3835 14 12.9431 13.7682 13.3556 13.3556C13.7682 12.9431 14 12.3835 14 11.8V2.2C14 1.61652 13.7682 1.05694 13.3556 0.644365C12.9431 0.231785 12.3835 0 11.8 0H2.2C1.61652 0 1.05694 0.231785 0.644365 0.644365C0.231785 1.05694 0 1.61652 0 2.2V7C0 7.15913 0.063214 7.31174 0.175736 7.42426C0.288258 7.53679 0.44087 7.6 0.6 7.6C0.75913 7.6 0.911742 7.53679 1.02426 7.42426C1.13679 7.31174 1.2 7.15913 1.2 7V2.2C1.2 1.93478 1.30536 1.68043 1.49289 1.49289C1.68043 1.30536 1.93478 1.2 2.2 1.2H11.8C12.0652 1.2 12.3196 1.30536 12.5071 1.49289C12.6946 1.68043 12.8 1.93478 12.8 2.2V11.8C12.8 12.0652 12.6946 12.3196 12.5071 12.5071C12.3196 12.6946 12.0652 12.8 11.8 12.8H7C6.84087 12.8 6.68826 12.8632 6.57574 12.9757C6.46321 13.0883 6.4 13.2409 6.4 13.4C6.4 13.5591 6.46321 13.7117 6.57574 13.8243C6.68826 13.9368 6.84087 14 7 14ZM9.77805 7.42192C9.89013 7.534 10.0415 7.59788 10.2 7.59995C10.3585 7.59788 10.5099 7.534 10.622 7.42192C10.7341 7.30985 10.798 7.15844 10.8 6.99995V3.94242C10.8066 3.90505 10.8096 3.86689 10.8089 3.82843C10.8079 3.77159 10.7988 3.7157 10.7824 3.6623C10.756 3.55552 10.701 3.45698 10.622 3.37798C10.5099 3.2659 10.3585 3.20202 10.2 3.19995H7.00002C6.84089 3.19995 6.68828 3.26317 6.57576 3.37569C6.46324 3.48821 6.40002 3.64082 6.40002 3.79995C6.40002 3.95908 6.46324 4.11169 6.57576 4.22422C6.68828 4.33674 6.84089 4.39995 7.00002 4.39995H8.80006L6.19997 7.00005C6.10158 7.11005 6.04718 7.25246 6.04718 7.40005C6.04718 7.54763 6.10158 7.69004 6.19997 7.80005C6.30202 7.91645 6.44561 7.98824 6.59997 8.00005C6.75432 7.98824 6.89791 7.91645 6.99997 7.80005L9.60002 5.26841V6.99995C9.6021 7.15844 9.66598 7.30985 9.77805 7.42192ZM1.4 14H3.8C4.17066 13.9979 4.52553 13.8498 4.78763 13.5877C5.04973 13.3256 5.1979 12.9707 5.2 12.6V10.2C5.1979 9.82939 5.04973 9.47452 4.78763 9.21242C4.52553 8.95032 4.17066 8.80215 3.8 8.80005H1.4C1.02934 8.80215 0.674468 8.95032 0.412371 9.21242C0.150274 9.47452 0.00210008 9.82939 0 10.2V12.6C0.00210008 12.9707 0.150274 13.3256 0.412371 13.5877C0.674468 13.8498 1.02934 13.9979 1.4 14ZM1.25858 10.0586C1.29609 10.0211 1.34696 10 1.4 10H3.8C3.85304 10 3.90391 10.0211 3.94142 10.0586C3.97893 10.0961 4 10.147 4 10.2V12.6C4 12.6531 3.97893 12.704 3.94142 12.7415C3.90391 12.779 3.85304 12.8 3.8 12.8H1.4C1.34696 12.8 1.29609 12.779 1.25858 12.7415C1.22107 12.704 1.2 12.6531 1.2 12.6V10.2C1.2 10.147 1.22107 10.0961 1.25858 10.0586Z",fill:"currentColor"},null,-1)]),16)}Ue.render=cn;var Ve={name:"WindowMinimizeIcon",extends:ne};function dn(e,t,n,s,r,o){return i(),d("svg",l({width:"14",height:"14",viewBox:"0 0 14 14",fill:"none",xmlns:"http://www.w3.org/2000/svg"},e.pti()),t[0]||(t[0]=[a("path",{"fill-rule":"evenodd","clip-rule":"evenodd",d:"M11.8 0H2.2C1.61652 0 1.05694 0.231785 0.644365 0.644365C0.231785 1.05694 0 1.61652 0 2.2V7C0 7.15913 0.063214 7.31174 0.175736 7.42426C0.288258 7.53679 0.44087 7.6 0.6 7.6C0.75913 7.6 0.911742 7.53679 1.02426 7.42426C1.13679 7.31174 1.2 7.15913 1.2 7V2.2C1.2 1.93478 1.30536 1.68043 1.49289 1.49289C1.68043 1.30536 1.93478 1.2 2.2 1.2H11.8C12.0652 1.2 12.3196 1.30536 12.5071 1.49289C12.6946 1.68043 12.8 1.93478 12.8 2.2V11.8C12.8 12.0652 12.6946 12.3196 12.5071 12.5071C12.3196 12.6946 12.0652 12.8 11.8 12.8H7C6.84087 12.8 6.68826 12.8632 6.57574 12.9757C6.46321 13.0883 6.4 13.2409 6.4 13.4C6.4 13.5591 6.46321 13.7117 6.57574 13.8243C6.68826 13.9368 6.84087 14 7 14H11.8C12.3835 14 12.9431 13.7682 13.3556 13.3556C13.7682 12.9431 14 12.3835 14 11.8V2.2C14 1.61652 13.7682 1.05694 13.3556 0.644365C12.9431 0.231785 12.3835 0 11.8 0ZM6.368 7.952C6.44137 7.98326 6.52025 7.99958 6.6 8H9.8C9.95913 8 10.1117 7.93678 10.2243 7.82426C10.3368 7.71174 10.4 7.55913 10.4 7.4C10.4 7.24087 10.3368 7.08826 10.2243 6.97574C10.1117 6.86321 9.95913 6.8 9.8 6.8H8.048L10.624 4.224C10.73 4.11026 10.7877 3.95982 10.7849 3.80438C10.7822 3.64894 10.7192 3.50063 10.6093 3.3907C10.4994 3.28077 10.3511 3.2178 10.1956 3.21506C10.0402 3.21232 9.88974 3.27002 9.776 3.376L7.2 5.952V4.2C7.2 4.04087 7.13679 3.88826 7.02426 3.77574C6.91174 3.66321 6.75913 3.6 6.6 3.6C6.44087 3.6 6.28826 3.66321 6.17574 3.77574C6.06321 3.88826 6 4.04087 6 4.2V7.4C6.00042 7.47975 6.01674 7.55862 6.048 7.632C6.07656 7.70442 6.11971 7.7702 6.17475 7.82524C6.2298 7.88029 6.29558 7.92344 6.368 7.952ZM1.4 8.80005H3.8C4.17066 8.80215 4.52553 8.95032 4.78763 9.21242C5.04973 9.47452 5.1979 9.82939 5.2 10.2V12.6C5.1979 12.9707 5.04973 13.3256 4.78763 13.5877C4.52553 13.8498 4.17066 13.9979 3.8 14H1.4C1.02934 13.9979 0.674468 13.8498 0.412371 13.5877C0.150274 13.3256 0.00210008 12.9707 0 12.6V10.2C0.00210008 9.82939 0.150274 9.47452 0.412371 9.21242C0.674468 8.95032 1.02934 8.80215 1.4 8.80005ZM3.94142 12.7415C3.97893 12.704 4 12.6531 4 12.6V10.2C4 10.147 3.97893 10.0961 3.94142 10.0586C3.90391 10.0211 3.85304 10 3.8 10H1.4C1.34696 10 1.29609 10.0211 1.25858 10.0586C1.22107 10.0961 1.2 10.147 1.2 10.2V12.6C1.2 12.6531 1.22107 12.704 1.25858 12.7415C1.29609 12.779 1.34696 12.8 1.4 12.8H3.8C3.85304 12.8 3.90391 12.779 3.94142 12.7415Z",fill:"currentColor"},null,-1)]),16)}Ve.render=dn;function ke(){pt({variableName:Me("scrollbar.width").name})}function xe(){mt({variableName:Me("scrollbar.width").name})}var un=`
    .p-dialog {
        max-height: 90%;
        transform: scale(1);
        border-radius: dt('dialog.border.radius');
        box-shadow: dt('dialog.shadow');
        background: dt('dialog.background');
        border: 1px solid dt('dialog.border.color');
        color: dt('dialog.color');
    }

    .p-dialog-content {
        overflow-y: auto;
        padding: dt('dialog.content.padding');
    }

    .p-dialog-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        flex-shrink: 0;
        padding: dt('dialog.header.padding');
    }

    .p-dialog-title {
        font-weight: dt('dialog.title.font.weight');
        font-size: dt('dialog.title.font.size');
    }

    .p-dialog-footer {
        flex-shrink: 0;
        padding: dt('dialog.footer.padding');
        display: flex;
        justify-content: flex-end;
        gap: dt('dialog.footer.gap');
    }

    .p-dialog-header-actions {
        display: flex;
        align-items: center;
        gap: dt('dialog.header.gap');
    }

    .p-dialog-enter-active {
        transition: all 150ms cubic-bezier(0, 0, 0.2, 1);
    }

    .p-dialog-leave-active {
        transition: all 150ms cubic-bezier(0.4, 0, 0.2, 1);
    }

    .p-dialog-enter-from,
    .p-dialog-leave-to {
        opacity: 0;
        transform: scale(0.7);
    }

    .p-dialog-top .p-dialog,
    .p-dialog-bottom .p-dialog,
    .p-dialog-left .p-dialog,
    .p-dialog-right .p-dialog,
    .p-dialog-topleft .p-dialog,
    .p-dialog-topright .p-dialog,
    .p-dialog-bottomleft .p-dialog,
    .p-dialog-bottomright .p-dialog {
        margin: 0.75rem;
        transform: translate3d(0px, 0px, 0px);
    }

    .p-dialog-top .p-dialog-enter-active,
    .p-dialog-top .p-dialog-leave-active,
    .p-dialog-bottom .p-dialog-enter-active,
    .p-dialog-bottom .p-dialog-leave-active,
    .p-dialog-left .p-dialog-enter-active,
    .p-dialog-left .p-dialog-leave-active,
    .p-dialog-right .p-dialog-enter-active,
    .p-dialog-right .p-dialog-leave-active,
    .p-dialog-topleft .p-dialog-enter-active,
    .p-dialog-topleft .p-dialog-leave-active,
    .p-dialog-topright .p-dialog-enter-active,
    .p-dialog-topright .p-dialog-leave-active,
    .p-dialog-bottomleft .p-dialog-enter-active,
    .p-dialog-bottomleft .p-dialog-leave-active,
    .p-dialog-bottomright .p-dialog-enter-active,
    .p-dialog-bottomright .p-dialog-leave-active {
        transition: all 0.3s ease-out;
    }

    .p-dialog-top .p-dialog-enter-from,
    .p-dialog-top .p-dialog-leave-to {
        transform: translate3d(0px, -100%, 0px);
    }

    .p-dialog-bottom .p-dialog-enter-from,
    .p-dialog-bottom .p-dialog-leave-to {
        transform: translate3d(0px, 100%, 0px);
    }

    .p-dialog-left .p-dialog-enter-from,
    .p-dialog-left .p-dialog-leave-to,
    .p-dialog-topleft .p-dialog-enter-from,
    .p-dialog-topleft .p-dialog-leave-to,
    .p-dialog-bottomleft .p-dialog-enter-from,
    .p-dialog-bottomleft .p-dialog-leave-to {
        transform: translate3d(-100%, 0px, 0px);
    }

    .p-dialog-right .p-dialog-enter-from,
    .p-dialog-right .p-dialog-leave-to,
    .p-dialog-topright .p-dialog-enter-from,
    .p-dialog-topright .p-dialog-leave-to,
    .p-dialog-bottomright .p-dialog-enter-from,
    .p-dialog-bottomright .p-dialog-leave-to {
        transform: translate3d(100%, 0px, 0px);
    }

    .p-dialog-left:dir(rtl) .p-dialog-enter-from,
    .p-dialog-left:dir(rtl) .p-dialog-leave-to,
    .p-dialog-topleft:dir(rtl) .p-dialog-enter-from,
    .p-dialog-topleft:dir(rtl) .p-dialog-leave-to,
    .p-dialog-bottomleft:dir(rtl) .p-dialog-enter-from,
    .p-dialog-bottomleft:dir(rtl) .p-dialog-leave-to {
        transform: translate3d(100%, 0px, 0px);
    }

    .p-dialog-right:dir(rtl) .p-dialog-enter-from,
    .p-dialog-right:dir(rtl) .p-dialog-leave-to,
    .p-dialog-topright:dir(rtl) .p-dialog-enter-from,
    .p-dialog-topright:dir(rtl) .p-dialog-leave-to,
    .p-dialog-bottomright:dir(rtl) .p-dialog-enter-from,
    .p-dialog-bottomright:dir(rtl) .p-dialog-leave-to {
        transform: translate3d(-100%, 0px, 0px);
    }

    .p-dialog-maximized {
        width: 100vw !important;
        height: 100vh !important;
        top: 0px !important;
        left: 0px !important;
        max-height: 100%;
        height: 100%;
        border-radius: 0;
    }

    .p-dialog-maximized .p-dialog-content {
        flex-grow: 1;
    }

    .p-dialog .p-resizable-handle {
        position: absolute;
        font-size: 0.1px;
        display: block;
        cursor: se-resize;
        width: 12px;
        height: 12px;
        right: 1px;
        bottom: 1px;
    }
`,mn={mask:function(t){var n=t.position,s=t.modal;return{position:"fixed",height:"100%",width:"100%",left:0,top:0,display:"flex",justifyContent:n==="left"||n==="topleft"||n==="bottomleft"?"flex-start":n==="right"||n==="topright"||n==="bottomright"?"flex-end":"center",alignItems:n==="top"||n==="topleft"||n==="topright"?"flex-start":n==="bottom"||n==="bottomleft"||n==="bottomright"?"flex-end":"center",pointerEvents:s?"auto":"none"}},root:{display:"flex",flexDirection:"column",pointerEvents:"auto"}},pn={mask:function(t){var n=t.props,s=["left","right","top","topleft","topright","bottom","bottomleft","bottomright"],r=s.find(function(o){return o===n.position});return["p-dialog-mask",{"p-overlay-mask p-overlay-mask-enter":n.modal},r?"p-dialog-".concat(r):""]},root:function(t){var n=t.props,s=t.instance;return["p-dialog p-component",{"p-dialog-maximized":n.maximizable&&s.maximized}]},header:"p-dialog-header",title:"p-dialog-title",headerActions:"p-dialog-header-actions",pcMaximizeButton:"p-dialog-maximize-button",pcCloseButton:"p-dialog-close-button",content:"p-dialog-content",footer:"p-dialog-footer"},fn=Z.extend({name:"dialog",style:un,classes:pn,inlineStyles:mn}),gn={name:"BaseDialog",extends:R,props:{header:{type:null,default:null},footer:{type:null,default:null},visible:{type:Boolean,default:!1},modal:{type:Boolean,default:null},contentStyle:{type:null,default:null},contentClass:{type:String,default:null},contentProps:{type:null,default:null},maximizable:{type:Boolean,default:!1},dismissableMask:{type:Boolean,default:!1},closable:{type:Boolean,default:!0},closeOnEscape:{type:Boolean,default:!0},showHeader:{type:Boolean,default:!0},blockScroll:{type:Boolean,default:!1},baseZIndex:{type:Number,default:0},autoZIndex:{type:Boolean,default:!0},position:{type:String,default:"center"},breakpoints:{type:Object,default:null},draggable:{type:Boolean,default:!0},keepInViewport:{type:Boolean,default:!0},minX:{type:Number,default:0},minY:{type:Number,default:0},appendTo:{type:[String,Object],default:"body"},closeIcon:{type:String,default:void 0},maximizeIcon:{type:String,default:void 0},minimizeIcon:{type:String,default:void 0},closeButtonProps:{type:Object,default:function(){return{severity:"secondary",text:!0,rounded:!0}}},maximizeButtonProps:{type:Object,default:function(){return{severity:"secondary",text:!0,rounded:!0}}},_instance:null},style:fn,provide:function(){return{$pcDialog:this,$parentInstance:this}}},Ne={name:"Dialog",extends:gn,inheritAttrs:!1,emits:["update:visible","show","hide","after-hide","maximize","unmaximize","dragstart","dragend"],provide:function(){var t=this;return{dialogRef:z(function(){return t._instance})}},data:function(){return{containerVisible:this.visible,maximized:!1,focusableMax:null,focusableClose:null,target:null}},documentKeydownListener:null,container:null,mask:null,content:null,headerContainer:null,footerContainer:null,maximizableButton:null,closeButton:null,styleElement:null,dragging:null,documentDragListener:null,documentDragEndListener:null,lastPageX:null,lastPageY:null,maskMouseDownTarget:null,updated:function(){this.visible&&(this.containerVisible=this.visible)},beforeUnmount:function(){this.unbindDocumentState(),this.unbindGlobalListeners(),this.destroyStyle(),this.mask&&this.autoZIndex&&N.clear(this.mask),this.container=null,this.mask=null},mounted:function(){this.breakpoints&&this.createStyle()},methods:{close:function(){this.$emit("update:visible",!1)},onEnter:function(){this.$emit("show"),this.target=document.activeElement,this.enableDocumentSettings(),this.bindGlobalListeners(),this.autoZIndex&&N.set("modal",this.mask,this.baseZIndex+this.$primevue.config.zIndex.modal)},onAfterEnter:function(){this.focus()},onBeforeLeave:function(){this.modal&&!this.isUnstyled&&vt(this.mask,"p-overlay-mask-leave"),this.dragging&&this.documentDragEndListener&&this.documentDragEndListener()},onLeave:function(){this.$emit("hide"),ye(this.target),this.target=null,this.focusableClose=null,this.focusableMax=null},onAfterLeave:function(){this.autoZIndex&&N.clear(this.mask),this.containerVisible=!1,this.unbindDocumentState(),this.unbindGlobalListeners(),this.$emit("after-hide")},onMaskMouseDown:function(t){this.maskMouseDownTarget=t.target},onMaskMouseUp:function(){this.dismissableMask&&this.modal&&this.mask===this.maskMouseDownTarget&&this.close()},focus:function(){var t=function(r){return r&&r.querySelector("[autofocus]")},n=this.$slots.footer&&t(this.footerContainer);n||(n=this.$slots.header&&t(this.headerContainer),n||(n=this.$slots.default&&t(this.content),n||(this.maximizable?(this.focusableMax=!0,n=this.maximizableButton):(this.focusableClose=!0,n=this.closeButton)))),n&&ye(n,{focusVisible:!0})},maximize:function(t){this.maximized?(this.maximized=!1,this.$emit("unmaximize",t)):(this.maximized=!0,this.$emit("maximize",t)),this.modal||(this.maximized?ke():xe())},enableDocumentSettings:function(){(this.modal||!this.modal&&this.blockScroll||this.maximizable&&this.maximized)&&ke()},unbindDocumentState:function(){(this.modal||!this.modal&&this.blockScroll||this.maximizable&&this.maximized)&&xe()},onKeyDown:function(t){t.code==="Escape"&&this.closeOnEscape&&this.close()},bindDocumentKeyDownListener:function(){this.documentKeydownListener||(this.documentKeydownListener=this.onKeyDown.bind(this),window.document.addEventListener("keydown",this.documentKeydownListener))},unbindDocumentKeyDownListener:function(){this.documentKeydownListener&&(window.document.removeEventListener("keydown",this.documentKeydownListener),this.documentKeydownListener=null)},containerRef:function(t){this.container=t},maskRef:function(t){this.mask=t},contentRef:function(t){this.content=t},headerContainerRef:function(t){this.headerContainer=t},footerContainerRef:function(t){this.footerContainer=t},maximizableRef:function(t){this.maximizableButton=t?t.$el:void 0},closeButtonRef:function(t){this.closeButton=t?t.$el:void 0},createStyle:function(){if(!this.styleElement&&!this.isUnstyled){var t;this.styleElement=document.createElement("style"),this.styleElement.type="text/css",Oe(this.styleElement,"nonce",(t=this.$primevue)===null||t===void 0||(t=t.config)===null||t===void 0||(t=t.csp)===null||t===void 0?void 0:t.nonce),document.head.appendChild(this.styleElement);var n="";for(var s in this.breakpoints)n+=`
                        @media screen and (max-width: `.concat(s,`) {
                            .p-dialog[`).concat(this.$attrSelector,`] {
                                width: `).concat(this.breakpoints[s],` !important;
                            }
                        }
                    `);this.styleElement.innerHTML=n}},destroyStyle:function(){this.styleElement&&(document.head.removeChild(this.styleElement),this.styleElement=null)},initDrag:function(t){t.target.closest("div").getAttribute("data-pc-section")!=="headeractions"&&this.draggable&&(this.dragging=!0,this.lastPageX=t.pageX,this.lastPageY=t.pageY,this.container.style.margin="0",document.body.setAttribute("data-p-unselectable-text","true"),!this.isUnstyled&&yt(document.body,{"user-select":"none"}),this.$emit("dragstart",t))},bindGlobalListeners:function(){this.draggable&&(this.bindDocumentDragListener(),this.bindDocumentDragEndListener()),this.closeOnEscape&&this.bindDocumentKeyDownListener()},unbindGlobalListeners:function(){this.unbindDocumentDragListener(),this.unbindDocumentDragEndListener(),this.unbindDocumentKeyDownListener()},bindDocumentDragListener:function(){var t=this;this.documentDragListener=function(n){if(t.dragging){var s=gt(t.container),r=ht(t.container),o=n.pageX-t.lastPageX,c=n.pageY-t.lastPageY,f=t.container.getBoundingClientRect(),u=f.left+o,p=f.top+c,$=bt(),S=getComputedStyle(t.container),D=parseFloat(S.marginLeft),g=parseFloat(S.marginTop);t.container.style.position="fixed",t.keepInViewport?(u>=t.minX&&u+s<$.width&&(t.lastPageX=n.pageX,t.container.style.left=u-D+"px"),p>=t.minY&&p+r<$.height&&(t.lastPageY=n.pageY,t.container.style.top=p-g+"px")):(t.lastPageX=n.pageX,t.container.style.left=u-D+"px",t.lastPageY=n.pageY,t.container.style.top=p-g+"px")}},window.document.addEventListener("mousemove",this.documentDragListener)},unbindDocumentDragListener:function(){this.documentDragListener&&(window.document.removeEventListener("mousemove",this.documentDragListener),this.documentDragListener=null)},bindDocumentDragEndListener:function(){var t=this;this.documentDragEndListener=function(n){t.dragging&&(t.dragging=!1,document.body.removeAttribute("data-p-unselectable-text"),!t.isUnstyled&&(document.body.style["user-select"]=""),t.$emit("dragend",n))},window.document.addEventListener("mouseup",this.documentDragEndListener)},unbindDocumentDragEndListener:function(){this.documentDragEndListener&&(window.document.removeEventListener("mouseup",this.documentDragEndListener),this.documentDragEndListener=null)}},computed:{maximizeIconComponent:function(){return this.maximized?this.minimizeIcon?"span":"WindowMinimizeIcon":this.maximizeIcon?"span":"WindowMaximizeIcon"},ariaLabelledById:function(){return this.header!=null||this.$attrs["aria-labelledby"]!==null?this.$id+"_header":null},closeAriaLabel:function(){return this.$primevue.config.locale.aria?this.$primevue.config.locale.aria.close:void 0},dataP:function(){return oe({maximized:this.maximized,modal:this.modal})}},directives:{ripple:fe,focustrap:ft},components:{Button:B,Portal:Te,WindowMinimizeIcon:Ve,WindowMaximizeIcon:Ue,TimesIcon:ge}};function J(e){"@babel/helpers - typeof";return J=typeof Symbol=="function"&&typeof Symbol.iterator=="symbol"?function(t){return typeof t}:function(t){return t&&typeof Symbol=="function"&&t.constructor===Symbol&&t!==Symbol.prototype?"symbol":typeof t},J(e)}function Se(e,t){var n=Object.keys(e);if(Object.getOwnPropertySymbols){var s=Object.getOwnPropertySymbols(e);t&&(s=s.filter(function(r){return Object.getOwnPropertyDescriptor(e,r).enumerable})),n.push.apply(n,s)}return n}function Pe(e){for(var t=1;t<arguments.length;t++){var n=arguments[t]!=null?arguments[t]:{};t%2?Se(Object(n),!0).forEach(function(s){hn(e,s,n[s])}):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(n)):Se(Object(n)).forEach(function(s){Object.defineProperty(e,s,Object.getOwnPropertyDescriptor(n,s))})}return e}function hn(e,t,n){return(t=bn(t))in e?Object.defineProperty(e,t,{value:n,enumerable:!0,configurable:!0,writable:!0}):e[t]=n,e}function bn(e){var t=yn(e,"string");return J(t)=="symbol"?t:t+""}function yn(e,t){if(J(e)!="object"||!e)return e;var n=e[Symbol.toPrimitive];if(n!==void 0){var s=n.call(e,t);if(J(s)!="object")return s;throw new TypeError("@@toPrimitive must return a primitive value.")}return(t==="string"?String:Number)(e)}var vn=["data-p"],wn=["aria-labelledby","aria-modal","data-p"],Cn=["id"],kn=["data-p"];function xn(e,t,n,s,r,o){var c=A("Button"),f=A("Portal"),u=ae("focustrap");return i(),x(f,{appendTo:e.appendTo},{default:P(function(){return[r.containerVisible?(i(),d("div",l({key:0,ref:o.maskRef,class:e.cx("mask"),style:e.sx("mask",!0,{position:e.position,modal:e.modal}),onMousedown:t[1]||(t[1]=function(){return o.onMaskMouseDown&&o.onMaskMouseDown.apply(o,arguments)}),onMouseup:t[2]||(t[2]=function(){return o.onMaskMouseUp&&o.onMaskMouseUp.apply(o,arguments)}),"data-p":o.dataP},e.ptm("mask")),[v(je,l({name:"p-dialog",onEnter:o.onEnter,onAfterEnter:o.onAfterEnter,onBeforeLeave:o.onBeforeLeave,onLeave:o.onLeave,onAfterLeave:o.onAfterLeave,appear:""},e.ptm("transition")),{default:P(function(){return[e.visible?M((i(),d("div",l({key:0,ref:o.containerRef,class:e.cx("root"),style:e.sx("root"),role:"dialog","aria-labelledby":o.ariaLabelledById,"aria-modal":e.modal,"data-p":o.dataP},e.ptmi("root")),[e.$slots.container?C(e.$slots,"container",{key:0,closeCallback:o.close,maximizeCallback:function($){return o.maximize($)}}):(i(),d(F,{key:1},[e.showHeader?(i(),d("div",l({key:0,ref:o.headerContainerRef,class:e.cx("header"),onMousedown:t[0]||(t[0]=function(){return o.initDrag&&o.initDrag.apply(o,arguments)})},e.ptm("header")),[C(e.$slots,"header",{class:K(e.cx("title"))},function(){return[e.header?(i(),d("span",l({key:0,id:o.ariaLabelledById,class:e.cx("title")},e.ptm("title")),w(e.header),17,Cn)):y("",!0)]}),a("div",l({class:e.cx("headerActions")},e.ptm("headerActions")),[e.maximizable?C(e.$slots,"maximizebutton",{key:0,maximized:r.maximized,maximizeCallback:function($){return o.maximize($)}},function(){return[v(c,l({ref:o.maximizableRef,autofocus:r.focusableMax,class:e.cx("pcMaximizeButton"),onClick:o.maximize,tabindex:e.maximizable?"0":"-1",unstyled:e.unstyled},e.maximizeButtonProps,{pt:e.ptm("pcMaximizeButton"),"data-pc-group-section":"headericon"}),{icon:P(function(p){return[C(e.$slots,"maximizeicon",{maximized:r.maximized},function(){return[(i(),x(I(o.maximizeIconComponent),l({class:[p.class,r.maximized?e.minimizeIcon:e.maximizeIcon]},e.ptm("pcMaximizeButton").icon),null,16,["class"]))]})]}),_:3},16,["autofocus","class","onClick","tabindex","unstyled","pt"])]}):y("",!0),e.closable?C(e.$slots,"closebutton",{key:1,closeCallback:o.close},function(){return[v(c,l({ref:o.closeButtonRef,autofocus:r.focusableClose,class:e.cx("pcCloseButton"),onClick:o.close,"aria-label":o.closeAriaLabel,unstyled:e.unstyled},e.closeButtonProps,{pt:e.ptm("pcCloseButton"),"data-pc-group-section":"headericon"}),{icon:P(function(p){return[C(e.$slots,"closeicon",{},function(){return[(i(),x(I(e.closeIcon?"span":"TimesIcon"),l({class:[e.closeIcon,p.class]},e.ptm("pcCloseButton").icon),null,16,["class"]))]})]}),_:3},16,["autofocus","class","onClick","aria-label","unstyled","pt"])]}):y("",!0)],16)],16)):y("",!0),a("div",l({ref:o.contentRef,class:[e.cx("content"),e.contentClass],style:e.contentStyle,"data-p":o.dataP},Pe(Pe({},e.contentProps),e.ptm("content"))),[C(e.$slots,"default")],16,kn),e.footer||e.$slots.footer?(i(),d("div",l({key:1,ref:o.footerContainerRef,class:e.cx("footer")},e.ptm("footer")),[C(e.$slots,"footer",{},function(){return[et(w(e.footer),1)]})],16)):y("",!0)],64))],16,wn)),[[u,{disabled:!e.modal}]]):y("",!0)]}),_:3},16,["onEnter","onAfterEnter","onBeforeLeave","onLeave","onAfterLeave"])],16,vn)):y("",!0)]}),_:3},8,["appendTo"])}Ne.render=xn;var Sn=`
    .p-confirmdialog .p-dialog-content {
        display: flex;
        align-items: center;
        gap: dt('confirmdialog.content.gap');
    }

    .p-confirmdialog-icon {
        color: dt('confirmdialog.icon.color');
        font-size: dt('confirmdialog.icon.size');
        width: dt('confirmdialog.icon.size');
        height: dt('confirmdialog.icon.size');
    }
`,Pn={root:"p-confirmdialog",icon:"p-confirmdialog-icon",message:"p-confirmdialog-message",pcRejectButton:"p-confirmdialog-reject-button",pcAcceptButton:"p-confirmdialog-accept-button"},$n=Z.extend({name:"confirmdialog",style:Sn,classes:Pn}),Ln={name:"BaseConfirmDialog",extends:R,props:{group:String,breakpoints:{type:Object,default:null},draggable:{type:Boolean,default:!0}},style:$n,provide:function(){return{$pcConfirmDialog:this,$parentInstance:this}}},Fe={name:"ConfirmDialog",extends:Ln,confirmListener:null,closeListener:null,data:function(){return{visible:!1,confirmation:null}},mounted:function(){var t=this;this.confirmListener=function(n){n&&n.group===t.group&&(t.confirmation=n,t.confirmation.onShow&&t.confirmation.onShow(),t.visible=!0)},this.closeListener=function(){t.visible=!1,t.confirmation=null},V.on("confirm",this.confirmListener),V.on("close",this.closeListener)},beforeUnmount:function(){V.off("confirm",this.confirmListener),V.off("close",this.closeListener)},methods:{accept:function(){this.confirmation.accept&&this.confirmation.accept(),this.visible=!1},reject:function(){this.confirmation.reject&&this.confirmation.reject(),this.visible=!1},onHide:function(){this.confirmation.onHide&&this.confirmation.onHide(),this.visible=!1}},computed:{appendTo:function(){return this.confirmation?this.confirmation.appendTo:"body"},target:function(){return this.confirmation?this.confirmation.target:null},modal:function(){return this.confirmation?this.confirmation.modal==null?!0:this.confirmation.modal:!0},header:function(){return this.confirmation?this.confirmation.header:null},message:function(){return this.confirmation?this.confirmation.message:null},blockScroll:function(){return this.confirmation?this.confirmation.blockScroll:!0},position:function(){return this.confirmation?this.confirmation.position:null},acceptLabel:function(){if(this.confirmation){var t,n=this.confirmation;return n.acceptLabel||((t=n.acceptProps)===null||t===void 0?void 0:t.label)||this.$primevue.config.locale.accept}return this.$primevue.config.locale.accept},rejectLabel:function(){if(this.confirmation){var t,n=this.confirmation;return n.rejectLabel||((t=n.rejectProps)===null||t===void 0?void 0:t.label)||this.$primevue.config.locale.reject}return this.$primevue.config.locale.reject},acceptIcon:function(){var t;return this.confirmation?this.confirmation.acceptIcon:(t=this.confirmation)!==null&&t!==void 0&&t.acceptProps?this.confirmation.acceptProps.icon:null},rejectIcon:function(){var t;return this.confirmation?this.confirmation.rejectIcon:(t=this.confirmation)!==null&&t!==void 0&&t.rejectProps?this.confirmation.rejectProps.icon:null},autoFocusAccept:function(){return this.confirmation.defaultFocus===void 0||this.confirmation.defaultFocus==="accept"},autoFocusReject:function(){return this.confirmation.defaultFocus==="reject"},closeOnEscape:function(){return this.confirmation?this.confirmation.closeOnEscape:!0}},components:{Dialog:Ne,Button:B}};function En(e,t,n,s,r,o){var c=A("Button"),f=A("Dialog");return i(),x(f,{visible:r.visible,"onUpdate:visible":[t[2]||(t[2]=function(u){return r.visible=u}),o.onHide],role:"alertdialog",class:K(e.cx("root")),modal:o.modal,header:o.header,blockScroll:o.blockScroll,appendTo:o.appendTo,position:o.position,breakpoints:e.breakpoints,closeOnEscape:o.closeOnEscape,draggable:e.draggable,pt:e.pt,unstyled:e.unstyled},ie({default:P(function(){return[e.$slots.container?y("",!0):(i(),d(F,{key:0},[e.$slots.message?(i(),x(I(e.$slots.message),{key:1,message:r.confirmation},null,8,["message"])):(i(),d(F,{key:0},[C(e.$slots,"icon",{},function(){return[e.$slots.icon?(i(),x(I(e.$slots.icon),{key:0,class:K(e.cx("icon"))},null,8,["class"])):r.confirmation.icon?(i(),d("span",l({key:1,class:[r.confirmation.icon,e.cx("icon")]},e.ptm("icon")),null,16)):y("",!0)]}),a("span",l({class:e.cx("message")},e.ptm("message")),w(o.message),17)],64))],64))]}),_:2},[e.$slots.container?{name:"container",fn:P(function(u){return[C(e.$slots,"container",{message:r.confirmation,closeCallback:u.onclose,acceptCallback:o.accept,rejectCallback:o.reject})]}),key:"0"}:void 0,e.$slots.container?void 0:{name:"footer",fn:P(function(){var u;return[v(c,l({class:[e.cx("pcRejectButton"),r.confirmation.rejectClass],autofocus:o.autoFocusReject,unstyled:e.unstyled,text:((u=r.confirmation.rejectProps)===null||u===void 0?void 0:u.text)||!1,onClick:t[0]||(t[0]=function(p){return o.reject()})},r.confirmation.rejectProps,{label:o.rejectLabel,pt:e.ptm("pcRejectButton")}),ie({_:2},[o.rejectIcon||e.$slots.rejecticon?{name:"icon",fn:P(function(p){return[C(e.$slots,"rejecticon",{},function(){return[a("span",l({class:[o.rejectIcon,p.class]},e.ptm("pcRejectButton").icon,{"data-pc-section":"rejectbuttonicon"}),null,16)]})]}),key:"0"}:void 0]),1040,["class","autofocus","unstyled","text","label","pt"]),v(c,l({label:o.acceptLabel,class:[e.cx("pcAcceptButton"),r.confirmation.acceptClass],autofocus:o.autoFocusAccept,unstyled:e.unstyled,onClick:t[1]||(t[1]=function(p){return o.accept()})},r.confirmation.acceptProps,{pt:e.ptm("pcAcceptButton")}),ie({_:2},[o.acceptIcon||e.$slots.accepticon?{name:"icon",fn:P(function(p){return[C(e.$slots,"accepticon",{},function(){return[a("span",l({class:[o.acceptIcon,p.class]},e.ptm("pcAcceptButton").icon,{"data-pc-section":"acceptbuttonicon"}),null,16)]})]}),key:"0"}:void 0]),1040,["label","class","autofocus","unstyled","pt"])]}),key:"1"}]),1032,["visible","class","modal","header","blockScroll","appendTo","position","breakpoints","closeOnEscape","draggable","onUpdate:visible","pt","unstyled"])}Fe.render=En;const zn={id:"app"},In=pe({__name:"App",setup(e){return(t,n)=>(i(),d("div",zn,[v(E(He)),v(E(Fe)),v(E(tt))]))}}),he=(e,t)=>{const n=e.__vccOpts||e;for(const[s,r]of t)n[s]=r;return n},jn=he(In,[["__scopeId","data-v-63285aa9"]]),Dn="modulepreload",Tn=function(e){return"/"+e},$e={},On=function(t,n,s){let r=Promise.resolve();if(n&&n.length>0){document.getElementsByTagName("link");const c=document.querySelector("meta[property=csp-nonce]"),f=(c==null?void 0:c.nonce)||(c==null?void 0:c.getAttribute("nonce"));r=Promise.allSettled(n.map(u=>{if(u=Tn(u),u in $e)return;$e[u]=!0;const p=u.endsWith(".css"),$=p?'[rel="stylesheet"]':"";if(document.querySelector(`link[href="${u}"]${$}`))return;const S=document.createElement("link");if(S.rel=p?"stylesheet":Dn,p||(S.as="script"),S.crossOrigin="",S.href=u,f&&S.setAttribute("nonce",f),document.head.appendChild(S),p)return new Promise((D,g)=>{S.addEventListener("load",D),S.addEventListener("error",()=>g(new Error(`Unable to preload CSS for ${u}`)))})}))}function o(c){const f=new Event("vite:preloadError",{cancelable:!0});if(f.payload=c,window.dispatchEvent(f),!f.defaultPrevented)throw c}return r.then(c=>{for(const f of c||[])f.status==="rejected"&&o(f.reason);return t().catch(o)})};var Mn=`
    .p-progressspinner {
        position: relative;
        margin: 0 auto;
        width: 100px;
        height: 100px;
        display: inline-block;
    }

    .p-progressspinner::before {
        content: '';
        display: block;
        padding-top: 100%;
    }

    .p-progressspinner-spin {
        height: 100%;
        transform-origin: center center;
        width: 100%;
        position: absolute;
        top: 0;
        bottom: 0;
        left: 0;
        right: 0;
        margin: auto;
        animation: p-progressspinner-rotate 2s linear infinite;
    }

    .p-progressspinner-circle {
        stroke-dasharray: 89, 200;
        stroke-dashoffset: 0;
        stroke: dt('progressspinner.colorOne');
        animation:
            p-progressspinner-dash 1.5s ease-in-out infinite,
            p-progressspinner-color 6s ease-in-out infinite;
        stroke-linecap: round;
    }

    @keyframes p-progressspinner-rotate {
        100% {
            transform: rotate(360deg);
        }
    }
    @keyframes p-progressspinner-dash {
        0% {
            stroke-dasharray: 1, 200;
            stroke-dashoffset: 0;
        }
        50% {
            stroke-dasharray: 89, 200;
            stroke-dashoffset: -35px;
        }
        100% {
            stroke-dasharray: 89, 200;
            stroke-dashoffset: -124px;
        }
    }
    @keyframes p-progressspinner-color {
        100%,
        0% {
            stroke: dt('progressspinner.color.one');
        }
        40% {
            stroke: dt('progressspinner.color.two');
        }
        66% {
            stroke: dt('progressspinner.color.three');
        }
        80%,
        90% {
            stroke: dt('progressspinner.color.four');
        }
    }
`,Bn={root:"p-progressspinner",spin:"p-progressspinner-spin",circle:"p-progressspinner-circle"},An=Z.extend({name:"progressspinner",style:Mn,classes:Bn}),Rn={name:"BaseProgressSpinner",extends:R,props:{strokeWidth:{type:String,default:"2"},fill:{type:String,default:"none"},animationDuration:{type:String,default:"2s"}},style:An,provide:function(){return{$pcProgressSpinner:this,$parentInstance:this}}},Ke={name:"ProgressSpinner",extends:Rn,inheritAttrs:!1,computed:{svgStyle:function(){return{"animation-duration":this.animationDuration}}}},Hn=["fill","stroke-width"];function Un(e,t,n,s,r,o){return i(),d("div",l({class:e.cx("root"),role:"progressbar"},e.ptmi("root")),[(i(),d("svg",l({class:e.cx("spin"),viewBox:"25 25 50 50",style:o.svgStyle},e.ptm("spin")),[a("circle",l({class:e.cx("circle"),cx:"50",cy:"50",r:"20",fill:e.fill,"stroke-width":e.strokeWidth,strokeMiterlimit:"10"},e.ptm("circle")),null,16,Hn)],16))],16)}Ke.render=Un;var Vn=`
    .p-message {
        border-radius: dt('message.border.radius');
        outline-width: dt('message.border.width');
        outline-style: solid;
    }

    .p-message-content {
        display: flex;
        align-items: center;
        padding: dt('message.content.padding');
        gap: dt('message.content.gap');
        height: 100%;
    }

    .p-message-icon {
        flex-shrink: 0;
    }

    .p-message-close-button {
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
        margin-inline-start: auto;
        overflow: hidden;
        position: relative;
        width: dt('message.close.button.width');
        height: dt('message.close.button.height');
        border-radius: dt('message.close.button.border.radius');
        background: transparent;
        transition:
            background dt('message.transition.duration'),
            color dt('message.transition.duration'),
            outline-color dt('message.transition.duration'),
            box-shadow dt('message.transition.duration'),
            opacity 0.3s;
        outline-color: transparent;
        color: inherit;
        padding: 0;
        border: none;
        cursor: pointer;
        user-select: none;
    }

    .p-message-close-icon {
        font-size: dt('message.close.icon.size');
        width: dt('message.close.icon.size');
        height: dt('message.close.icon.size');
    }

    .p-message-close-button:focus-visible {
        outline-width: dt('message.close.button.focus.ring.width');
        outline-style: dt('message.close.button.focus.ring.style');
        outline-offset: dt('message.close.button.focus.ring.offset');
    }

    .p-message-info {
        background: dt('message.info.background');
        outline-color: dt('message.info.border.color');
        color: dt('message.info.color');
        box-shadow: dt('message.info.shadow');
    }

    .p-message-info .p-message-close-button:focus-visible {
        outline-color: dt('message.info.close.button.focus.ring.color');
        box-shadow: dt('message.info.close.button.focus.ring.shadow');
    }

    .p-message-info .p-message-close-button:hover {
        background: dt('message.info.close.button.hover.background');
    }

    .p-message-info.p-message-outlined {
        color: dt('message.info.outlined.color');
        outline-color: dt('message.info.outlined.border.color');
    }

    .p-message-info.p-message-simple {
        color: dt('message.info.simple.color');
    }

    .p-message-success {
        background: dt('message.success.background');
        outline-color: dt('message.success.border.color');
        color: dt('message.success.color');
        box-shadow: dt('message.success.shadow');
    }

    .p-message-success .p-message-close-button:focus-visible {
        outline-color: dt('message.success.close.button.focus.ring.color');
        box-shadow: dt('message.success.close.button.focus.ring.shadow');
    }

    .p-message-success .p-message-close-button:hover {
        background: dt('message.success.close.button.hover.background');
    }

    .p-message-success.p-message-outlined {
        color: dt('message.success.outlined.color');
        outline-color: dt('message.success.outlined.border.color');
    }

    .p-message-success.p-message-simple {
        color: dt('message.success.simple.color');
    }

    .p-message-warn {
        background: dt('message.warn.background');
        outline-color: dt('message.warn.border.color');
        color: dt('message.warn.color');
        box-shadow: dt('message.warn.shadow');
    }

    .p-message-warn .p-message-close-button:focus-visible {
        outline-color: dt('message.warn.close.button.focus.ring.color');
        box-shadow: dt('message.warn.close.button.focus.ring.shadow');
    }

    .p-message-warn .p-message-close-button:hover {
        background: dt('message.warn.close.button.hover.background');
    }

    .p-message-warn.p-message-outlined {
        color: dt('message.warn.outlined.color');
        outline-color: dt('message.warn.outlined.border.color');
    }

    .p-message-warn.p-message-simple {
        color: dt('message.warn.simple.color');
    }

    .p-message-error {
        background: dt('message.error.background');
        outline-color: dt('message.error.border.color');
        color: dt('message.error.color');
        box-shadow: dt('message.error.shadow');
    }

    .p-message-error .p-message-close-button:focus-visible {
        outline-color: dt('message.error.close.button.focus.ring.color');
        box-shadow: dt('message.error.close.button.focus.ring.shadow');
    }

    .p-message-error .p-message-close-button:hover {
        background: dt('message.error.close.button.hover.background');
    }

    .p-message-error.p-message-outlined {
        color: dt('message.error.outlined.color');
        outline-color: dt('message.error.outlined.border.color');
    }

    .p-message-error.p-message-simple {
        color: dt('message.error.simple.color');
    }

    .p-message-secondary {
        background: dt('message.secondary.background');
        outline-color: dt('message.secondary.border.color');
        color: dt('message.secondary.color');
        box-shadow: dt('message.secondary.shadow');
    }

    .p-message-secondary .p-message-close-button:focus-visible {
        outline-color: dt('message.secondary.close.button.focus.ring.color');
        box-shadow: dt('message.secondary.close.button.focus.ring.shadow');
    }

    .p-message-secondary .p-message-close-button:hover {
        background: dt('message.secondary.close.button.hover.background');
    }

    .p-message-secondary.p-message-outlined {
        color: dt('message.secondary.outlined.color');
        outline-color: dt('message.secondary.outlined.border.color');
    }

    .p-message-secondary.p-message-simple {
        color: dt('message.secondary.simple.color');
    }

    .p-message-contrast {
        background: dt('message.contrast.background');
        outline-color: dt('message.contrast.border.color');
        color: dt('message.contrast.color');
        box-shadow: dt('message.contrast.shadow');
    }

    .p-message-contrast .p-message-close-button:focus-visible {
        outline-color: dt('message.contrast.close.button.focus.ring.color');
        box-shadow: dt('message.contrast.close.button.focus.ring.shadow');
    }

    .p-message-contrast .p-message-close-button:hover {
        background: dt('message.contrast.close.button.hover.background');
    }

    .p-message-contrast.p-message-outlined {
        color: dt('message.contrast.outlined.color');
        outline-color: dt('message.contrast.outlined.border.color');
    }

    .p-message-contrast.p-message-simple {
        color: dt('message.contrast.simple.color');
    }

    .p-message-text {
        font-size: dt('message.text.font.size');
        font-weight: dt('message.text.font.weight');
    }

    .p-message-icon {
        font-size: dt('message.icon.size');
        width: dt('message.icon.size');
        height: dt('message.icon.size');
    }

    .p-message-enter-from {
        opacity: 0;
    }

    .p-message-enter-active {
        transition: opacity 0.3s;
    }

    .p-message.p-message-leave-from {
        max-height: 1000px;
    }

    .p-message.p-message-leave-to {
        max-height: 0;
        opacity: 0;
        margin: 0;
    }

    .p-message-leave-active {
        overflow: hidden;
        transition:
            max-height 0.45s cubic-bezier(0, 1, 0, 1),
            opacity 0.3s,
            margin 0.3s;
    }

    .p-message-leave-active .p-message-close-button {
        opacity: 0;
    }

    .p-message-sm .p-message-content {
        padding: dt('message.content.sm.padding');
    }

    .p-message-sm .p-message-text {
        font-size: dt('message.text.sm.font.size');
    }

    .p-message-sm .p-message-icon {
        font-size: dt('message.icon.sm.size');
        width: dt('message.icon.sm.size');
        height: dt('message.icon.sm.size');
    }

    .p-message-sm .p-message-close-icon {
        font-size: dt('message.close.icon.sm.size');
        width: dt('message.close.icon.sm.size');
        height: dt('message.close.icon.sm.size');
    }

    .p-message-lg .p-message-content {
        padding: dt('message.content.lg.padding');
    }

    .p-message-lg .p-message-text {
        font-size: dt('message.text.lg.font.size');
    }

    .p-message-lg .p-message-icon {
        font-size: dt('message.icon.lg.size');
        width: dt('message.icon.lg.size');
        height: dt('message.icon.lg.size');
    }

    .p-message-lg .p-message-close-icon {
        font-size: dt('message.close.icon.lg.size');
        width: dt('message.close.icon.lg.size');
        height: dt('message.close.icon.lg.size');
    }

    .p-message-outlined {
        background: transparent;
        outline-width: dt('message.outlined.border.width');
    }

    .p-message-simple {
        background: transparent;
        outline-color: transparent;
        box-shadow: none;
    }

    .p-message-simple .p-message-content {
        padding: dt('message.simple.content.padding');
    }

    .p-message-outlined .p-message-close-button:hover,
    .p-message-simple .p-message-close-button:hover {
        background: transparent;
    }
`,Nn={root:function(t){var n=t.props;return["p-message p-component p-message-"+n.severity,{"p-message-outlined":n.variant==="outlined","p-message-simple":n.variant==="simple","p-message-sm":n.size==="small","p-message-lg":n.size==="large"}]},content:"p-message-content",icon:"p-message-icon",text:"p-message-text",closeButton:"p-message-close-button",closeIcon:"p-message-close-icon"},Fn=Z.extend({name:"message",style:Vn,classes:Nn}),Kn={name:"BaseMessage",extends:R,props:{severity:{type:String,default:"info"},closable:{type:Boolean,default:!1},life:{type:Number,default:null},icon:{type:String,default:void 0},closeIcon:{type:String,default:void 0},closeButtonProps:{type:null,default:null},size:{type:String,default:null},variant:{type:String,default:null}},style:Fn,provide:function(){return{$pcMessage:this,$parentInstance:this}}};function Q(e){"@babel/helpers - typeof";return Q=typeof Symbol=="function"&&typeof Symbol.iterator=="symbol"?function(t){return typeof t}:function(t){return t&&typeof Symbol=="function"&&t.constructor===Symbol&&t!==Symbol.prototype?"symbol":typeof t},Q(e)}function Le(e,t,n){return(t=Zn(t))in e?Object.defineProperty(e,t,{value:n,enumerable:!0,configurable:!0,writable:!0}):e[t]=n,e}function Zn(e){var t=Wn(e,"string");return Q(t)=="symbol"?t:t+""}function Wn(e,t){if(Q(e)!="object"||!e)return e;var n=e[Symbol.toPrimitive];if(n!==void 0){var s=n.call(e,t);if(Q(s)!="object")return s;throw new TypeError("@@toPrimitive must return a primitive value.")}return(t==="string"?String:Number)(e)}var Ze={name:"Message",extends:Kn,inheritAttrs:!1,emits:["close","life-end"],timeout:null,data:function(){return{visible:!0}},mounted:function(){var t=this;this.life&&setTimeout(function(){t.visible=!1,t.$emit("life-end")},this.life)},methods:{close:function(t){this.visible=!1,this.$emit("close",t)}},computed:{closeAriaLabel:function(){return this.$primevue.config.locale.aria?this.$primevue.config.locale.aria.close:void 0},dataP:function(){return oe(Le(Le({outlined:this.variant==="outlined",simple:this.variant==="simple"},this.severity,this.severity),this.size,this.size))}},directives:{ripple:fe},components:{TimesIcon:ge}};function ee(e){"@babel/helpers - typeof";return ee=typeof Symbol=="function"&&typeof Symbol.iterator=="symbol"?function(t){return typeof t}:function(t){return t&&typeof Symbol=="function"&&t.constructor===Symbol&&t!==Symbol.prototype?"symbol":typeof t},ee(e)}function Ee(e,t){var n=Object.keys(e);if(Object.getOwnPropertySymbols){var s=Object.getOwnPropertySymbols(e);t&&(s=s.filter(function(r){return Object.getOwnPropertyDescriptor(e,r).enumerable})),n.push.apply(n,s)}return n}function ze(e){for(var t=1;t<arguments.length;t++){var n=arguments[t]!=null?arguments[t]:{};t%2?Ee(Object(n),!0).forEach(function(s){Gn(e,s,n[s])}):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(n)):Ee(Object(n)).forEach(function(s){Object.defineProperty(e,s,Object.getOwnPropertyDescriptor(n,s))})}return e}function Gn(e,t,n){return(t=Yn(t))in e?Object.defineProperty(e,t,{value:n,enumerable:!0,configurable:!0,writable:!0}):e[t]=n,e}function Yn(e){var t=Xn(e,"string");return ee(t)=="symbol"?t:t+""}function Xn(e,t){if(ee(e)!="object"||!e)return e;var n=e[Symbol.toPrimitive];if(n!==void 0){var s=n.call(e,t);if(ee(s)!="object")return s;throw new TypeError("@@toPrimitive must return a primitive value.")}return(t==="string"?String:Number)(e)}var qn=["data-p"],_n=["data-p"],Jn=["data-p"],Qn=["aria-label","data-p"],eo=["data-p"];function to(e,t,n,s,r,o){var c=A("TimesIcon"),f=ae("ripple");return i(),x(je,l({name:"p-message",appear:""},e.ptmi("transition")),{default:P(function(){return[M(a("div",l({class:e.cx("root"),role:"alert","aria-live":"assertive","aria-atomic":"true","data-p":o.dataP},e.ptm("root")),[e.$slots.container?C(e.$slots,"container",{key:0,closeCallback:o.close}):(i(),d("div",l({key:1,class:e.cx("content"),"data-p":o.dataP},e.ptm("content")),[C(e.$slots,"icon",{class:K(e.cx("icon"))},function(){return[(i(),x(I(e.icon?"span":null),l({class:[e.cx("icon"),e.icon],"data-p":o.dataP},e.ptm("icon")),null,16,["class","data-p"]))]}),e.$slots.default?(i(),d("div",l({key:0,class:e.cx("text"),"data-p":o.dataP},e.ptm("text")),[C(e.$slots,"default")],16,Jn)):y("",!0),e.closable?M((i(),d("button",l({key:1,class:e.cx("closeButton"),"aria-label":o.closeAriaLabel,type:"button",onClick:t[0]||(t[0]=function(u){return o.close(u)}),"data-p":o.dataP},ze(ze({},e.closeButtonProps),e.ptm("closeButton"))),[C(e.$slots,"closeicon",{},function(){return[e.closeIcon?(i(),d("i",l({key:0,class:[e.cx("closeIcon"),e.closeIcon],"data-p":o.dataP},e.ptm("closeIcon")),null,16,eo)):(i(),x(c,l({key:1,class:[e.cx("closeIcon"),e.closeIcon],"data-p":o.dataP},e.ptm("closeIcon")),null,16,["class","data-p"]))]})],16,Qn)),[[f]]):y("",!0)],16,_n))],16,qn),[[nt,r.visible]])]}),_:3},16)}Ze.render=to;var no=`
    .p-tag {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        background: dt('tag.primary.background');
        color: dt('tag.primary.color');
        font-size: dt('tag.font.size');
        font-weight: dt('tag.font.weight');
        padding: dt('tag.padding');
        border-radius: dt('tag.border.radius');
        gap: dt('tag.gap');
    }

    .p-tag-icon {
        font-size: dt('tag.icon.size');
        width: dt('tag.icon.size');
        height: dt('tag.icon.size');
    }

    .p-tag-rounded {
        border-radius: dt('tag.rounded.border.radius');
    }

    .p-tag-success {
        background: dt('tag.success.background');
        color: dt('tag.success.color');
    }

    .p-tag-info {
        background: dt('tag.info.background');
        color: dt('tag.info.color');
    }

    .p-tag-warn {
        background: dt('tag.warn.background');
        color: dt('tag.warn.color');
    }

    .p-tag-danger {
        background: dt('tag.danger.background');
        color: dt('tag.danger.color');
    }

    .p-tag-secondary {
        background: dt('tag.secondary.background');
        color: dt('tag.secondary.color');
    }

    .p-tag-contrast {
        background: dt('tag.contrast.background');
        color: dt('tag.contrast.color');
    }
`,oo={root:function(t){var n=t.props;return["p-tag p-component",{"p-tag-info":n.severity==="info","p-tag-success":n.severity==="success","p-tag-warn":n.severity==="warn","p-tag-danger":n.severity==="danger","p-tag-secondary":n.severity==="secondary","p-tag-contrast":n.severity==="contrast","p-tag-rounded":n.rounded}]},icon:"p-tag-icon",label:"p-tag-label"},so=Z.extend({name:"tag",style:no,classes:oo}),ro={name:"BaseTag",extends:R,props:{value:null,severity:null,rounded:Boolean,icon:String},style:so,provide:function(){return{$pcTag:this,$parentInstance:this}}};function te(e){"@babel/helpers - typeof";return te=typeof Symbol=="function"&&typeof Symbol.iterator=="symbol"?function(t){return typeof t}:function(t){return t&&typeof Symbol=="function"&&t.constructor===Symbol&&t!==Symbol.prototype?"symbol":typeof t},te(e)}function ao(e,t,n){return(t=io(t))in e?Object.defineProperty(e,t,{value:n,enumerable:!0,configurable:!0,writable:!0}):e[t]=n,e}function io(e){var t=lo(e,"string");return te(t)=="symbol"?t:t+""}function lo(e,t){if(te(e)!="object"||!e)return e;var n=e[Symbol.toPrimitive];if(n!==void 0){var s=n.call(e,t);if(te(s)!="object")return s;throw new TypeError("@@toPrimitive must return a primitive value.")}return(t==="string"?String:Number)(e)}var We={name:"Tag",extends:ro,inheritAttrs:!1,computed:{dataP:function(){return oe(ao({rounded:this.rounded},this.severity,this.severity))}}},co=["data-p"];function uo(e,t,n,s,r,o){return i(),d("span",l({class:e.cx("root"),"data-p":o.dataP},e.ptmi("root")),[e.$slots.icon?(i(),x(I(e.$slots.icon),l({key:0,class:e.cx("icon")},e.ptm("icon")),null,16,["class"])):e.icon?(i(),d("span",l({key:1,class:[e.cx("icon"),e.icon]},e.ptm("icon")),null,16)):y("",!0),e.value!=null||e.$slots.default?C(e.$slots,"default",{key:2},function(){return[a("span",l({class:e.cx("label")},e.ptm("label")),w(e.value),17)]}):y("",!0)],16,co)}We.render=uo;const U="/api";class me{static async getCrawlerConfigurations(){const t=await fetch(`${U}/crawlers`);if(!t.ok)throw new Error(`Failed to fetch crawler configurations: ${t.statusText}`);return t.json()}static async getAllCrawlerHealth(){const t=await fetch(`${U}/health`);if(!t.ok)throw new Error(`Failed to fetch crawler health: ${t.statusText}`);return t.json()}static async getCrawlerHealth(t){const n=await fetch(`${U}/health/${t}`);if(!n.ok)throw new Error(`Failed to fetch health for crawler ${t}: ${n.statusText}`);return n.json()}static async forceHealthCheck(t){const n=await fetch(`${U}/health/${t}/check`,{method:"POST"});if(!n.ok)throw new Error(`Failed to force health check for crawler ${t}: ${n.statusText}`);return n.json()}static async triggerCrawl(t,n){const s={crawlerId:t,...n},r=await fetch(`${U}/crawlers/${t}/crawl`,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(s)});if(!r.ok){let o=`Failed to trigger crawl for ${t}: ${r.statusText}`;try{const c=await r.json();c.message&&(o=c.message)}catch{}throw new Error(o)}return r.json()}static async getCrawlerStatus(t){const n=await fetch(`${U}/crawlers/${t}/status`);if(!n.ok)throw new Error(`Failed to get status for crawler ${t}: ${n.statusText}`);return n.json()}}const mo={class:"crawler-health-card"},po={class:"flex align-items-center justify-content-between p-3 pb-0"},fo={class:"flex align-items-center"},go={class:"m-0 text-900 font-semibold"},ho={class:"m-0 text-600 text-sm"},bo={class:"flex align-items-center"},yo={class:"p-3 pt-0"},vo={class:"grid"},wo={class:"col-6"},Co={class:"flex align-items-center"},ko={class:"col-6"},xo={class:"text-900 font-medium"},So={class:"col-12 mt-2"},Po={class:"text-900 text-sm"},$o={key:0,class:"col-12 mt-2"},Lo={class:"text-900 text-sm"},Eo={class:"mt-3"},zo={class:"flex align-items-center"},Io={class:"text-900 text-sm mr-2"},jo=pe({__name:"CrawlerHealthCard",props:{crawler:{},health:{}},emits:["healthUpdated"],setup(e,{emit:t}){const n=e,s=t;ot();const r=Ae(),o=O(!1),c=z(()=>{switch(n.health.status){case"HEALTHY":return"health-healthy";case"UNHEALTHY":return"health-unhealthy";default:return"health-unknown"}}),f=z(()=>{switch(n.health.status){case"HEALTHY":return"pi pi-check-circle";case"UNHEALTHY":return"pi pi-times-circle";default:return"pi pi-question-circle"}}),u=z(()=>{switch(n.health.status){case"HEALTHY":return"success";case"UNHEALTHY":return"danger";default:return"warning"}}),p=z(()=>n.health.responseTimeMs===null?"N/A":`${n.health.responseTimeMs}ms`),$=z(()=>{if(!n.health.lastCheck)return"Never";const g=new Date(n.health.lastCheck),j=new Date().getTime()-g.getTime(),T=Math.floor(j/1e3),se=Math.floor(T/60);return T<60?`${T}s ago`:se<60?`${se}m ago`:g.toLocaleString()}),S=async()=>{o.value=!0;try{const g=await me.forceHealthCheck(n.crawler.id);s("healthUpdated",g),r.add({severity:"success",summary:"Health Check",detail:`Forced health check for ${n.crawler.name}`,life:3e3})}catch(g){console.error("Failed to force health check:",g),r.add({severity:"error",summary:"Error",detail:"Failed to perform health check",life:5e3})}finally{o.value=!1}},D=async g=>{try{await navigator.clipboard.writeText(g),r.add({severity:"success",summary:"Copied",detail:"URL copied to clipboard",life:2e3})}catch(b){console.error("Failed to copy to clipboard:",b),r.add({severity:"error",summary:"Error",detail:"Failed to copy URL",life:3e3})}};return(g,b)=>{const j=ae("tooltip");return i(),d("div",mo,[v(E(wt),{class:"h-full"},{header:P(()=>[a("div",po,[a("div",fo,[a("div",{class:K(["health-indicator mr-3",c.value])},[a("i",{class:K(f.value)},null,2)],2),a("div",null,[a("h6",go,w(g.crawler.name),1),a("p",ho,w(g.crawler.id),1)])]),a("div",bo,[M(v(E(B),{icon:"pi pi-refresh",class:"p-button-text p-button-sm",loading:o.value,onClick:S},null,8,["loading"]),[[j,"Force health check",void 0,{top:!0}]]),M(v(E(B),{icon:"pi pi-external-link",class:"p-button-text p-button-sm ml-2",onClick:b[0]||(b[0]=T=>g.$router.push(`/crawler/${g.crawler.id}`))},null,512),[[j,"View details",void 0,{top:!0}]])])])]),content:P(()=>[a("div",yo,[a("div",vo,[a("div",wo,[b[2]||(b[2]=a("div",{class:"text-600 text-sm mb-1"},"Status",-1)),a("div",Co,[v(E(We),{value:g.health.status,severity:u.value,class:"text-xs"},null,8,["value","severity"])])]),a("div",ko,[b[3]||(b[3]=a("div",{class:"text-600 text-sm mb-1"},"Response Time",-1)),a("div",xo,w(p.value),1)]),a("div",So,[b[4]||(b[4]=a("div",{class:"text-600 text-sm mb-1"},"Last Check",-1)),a("div",Po,w($.value),1)]),g.health.message?(i(),d("div",$o,[b[5]||(b[5]=a("div",{class:"text-600 text-sm mb-1"},"Message",-1)),a("div",Lo,w(g.health.message),1)])):y("",!0)]),a("div",Eo,[b[6]||(b[6]=a("div",{class:"text-600 text-sm mb-2"},"Endpoint",-1)),a("div",zo,[a("span",Io,w(g.crawler.baseUrl),1),M(v(E(B),{icon:"pi pi-copy",class:"p-button-text p-button-sm",onClick:b[1]||(b[1]=T=>D(g.crawler.baseUrl))},null,512),[[j,"Copy URL",void 0,{top:!0}]])])])])]),_:1})])}}}),Do=he(jo,[["__scopeId","data-v-6429d1be"]]);class To{constructor(){H(this,"ws",null);H(this,"reconnectAttempts",0);H(this,"maxReconnectAttempts",5);H(this,"reconnectDelay",1e3);H(this,"listeners",[]);this.connect()}connect(){const n=`${window.location.protocol==="https:"?"wss:":"ws:"}//${window.location.host}/ws/health`;try{this.ws=new WebSocket(n),this.ws.onopen=()=>{console.log("Health WebSocket connected"),this.reconnectAttempts=0},this.ws.onmessage=s=>{try{const r=JSON.parse(s.data);this.listeners.forEach(o=>o(r))}catch(r){console.error("Failed to parse WebSocket message:",r)}},this.ws.onclose=()=>{console.log("Health WebSocket disconnected"),this.attemptReconnect()},this.ws.onerror=s=>{console.error("Health WebSocket error:",s)}}catch(s){console.error("Failed to create WebSocket connection:",s),this.attemptReconnect()}}attemptReconnect(){this.reconnectAttempts<this.maxReconnectAttempts?(this.reconnectAttempts++,console.log(`Attempting to reconnect WebSocket (${this.reconnectAttempts}/${this.maxReconnectAttempts})`),setTimeout(()=>{this.connect()},this.reconnectDelay*this.reconnectAttempts)):console.error("Max WebSocket reconnection attempts reached")}onHealthUpdate(t){return this.listeners.push(t),()=>{const n=this.listeners.indexOf(t);n>-1&&this.listeners.splice(n,1)}}disconnect(){this.ws&&(this.ws.close(),this.ws=null),this.listeners=[]}}const Oo={class:"dashboard"},Mo={class:"surface-0 shadow-2 p-3 border-1 border-50 border-round"},Bo={class:"grid mb-4"},Ao={class:"col-12 md:col-3"},Ro={class:"card text-center"},Ho={class:"text-2xl font-bold text-900"},Uo={class:"col-12 md:col-3"},Vo={class:"card text-center"},No={class:"text-2xl font-bold text-green-500"},Fo={class:"col-12 md:col-3"},Ko={class:"card text-center"},Zo={class:"text-2xl font-bold text-red-500"},Wo={class:"col-12 md:col-3"},Go={class:"card text-center"},Yo={class:"text-2xl font-bold text-orange-500"},Xo={class:"flex justify-content-between align-items-center mb-4"},qo={class:"flex align-items-center"},_o={class:"flex align-items-center mr-3"},Jo={key:0,class:"pi pi-circle-fill text-red-500 mr-1"},Qo={key:1,class:"text-sm text-600 mr-3"},es={key:0,class:"flex align-items-center justify-content-center p-6"},ts={key:1,class:"card"},ns={class:"flex align-items-center"},os={class:"text-sm"},ss={class:"text-center mt-3"},rs={key:2,class:"card"},as={key:3,class:"grid"},is={key:4,class:"text-center mt-4"},ls={class:"text-600"},cs=pe({__name:"Dashboard",setup(e){const t=Ae(),n=O(!0),s=O(!1),r=O(null),o=O([]),c=O(null),f=O(!0);let u=null,p=null;const $=z(()=>o.value.length),S=z(()=>o.value.filter(h=>h.health.status==="HEALTHY").length),D=z(()=>o.value.filter(h=>h.health.status==="UNHEALTHY").length),g=z(()=>o.value.filter(h=>h.health.status==="UNKNOWN").length),b=z(()=>{if(!c.value)return"Never";const m=new Date().getTime()-c.value.getTime(),k=Math.floor(m/1e3);return k<60?`${k}s ago`:k<3600?`${Math.floor(k/60)}m ago`:c.value.toLocaleString()}),j=async()=>{try{r.value=null,n.value=!0;const[h,m]=await Promise.all([me.getCrawlerConfigurations(),me.getAllCrawlerHealth()]);o.value=h.map(k=>({configuration:k,health:m[k.id]||{status:"UNKNOWN",message:"No health data available",lastCheck:"",responseTimeMs:null,crawlerId:k.id}})),c.value=new Date,o.value.length>0&&t.add({severity:"success",summary:"Data Loaded",detail:`Loaded ${o.value.length} crawler(s)`,life:3e3})}catch(h){console.error("Failed to load data:",h),r.value=h instanceof Error?h.message:"Unknown error occurred",t.add({severity:"error",summary:"Load Error",detail:"Failed to load crawler data",life:5e3})}finally{n.value=!1}},T=async()=>{s.value=!0;try{await j()}finally{s.value=!1}},se=h=>{const m=o.value.findIndex(k=>k.configuration.id===h.crawlerId);m!==-1&&(o.value[m].health=h,c.value=new Date)},Ge=h=>{const m=o.value.findIndex(k=>k.configuration.id===h.crawlerId);m!==-1&&(o.value[m].health={status:h.status,message:h.message,lastCheck:h.timestamp,responseTimeMs:null,crawlerId:h.crawlerId},c.value=new Date)},Ye=()=>{try{u=new To,p=u.onHealthUpdate(Ge),f.value=!0}catch(h){console.error("Failed to initialize WebSocket:",h),f.value=!1}};return st(async()=>{await j(),Ye()}),rt(()=>{p&&p(),u&&u.disconnect()}),(h,m)=>(i(),d("div",Oo,[a("div",Mo,[m[11]||(m[11]=a("div",{class:"text-center mb-5"},[a("div",{class:"text-900 text-3xl font-medium mb-3"},"Crawler Manager Dashboard"),a("span",{class:"text-600 text-2xl"},"Monitor and control your web crawlers")],-1)),a("div",Bo,[a("div",Ao,[a("div",Ro,[a("div",Ho,w($.value),1),m[0]||(m[0]=a("div",{class:"text-600"},"Total Crawlers",-1))])]),a("div",Uo,[a("div",Vo,[a("div",No,w(S.value),1),m[1]||(m[1]=a("div",{class:"text-600"},"Healthy",-1))])]),a("div",Fo,[a("div",Ko,[a("div",Zo,w(D.value),1),m[2]||(m[2]=a("div",{class:"text-600"},"Unhealthy",-1))])]),a("div",Wo,[a("div",Go,[a("div",Yo,w(g.value),1),m[3]||(m[3]=a("div",{class:"text-600"},"Unknown",-1))])])]),a("div",Xo,[m[6]||(m[6]=a("h5",{class:"m-0"},"Crawler Status",-1)),a("div",qo,[a("div",_o,[m[4]||(m[4]=a("i",{class:"pi pi-circle-fill text-green-500 mr-1"},null,-1)),m[5]||(m[5]=a("span",{class:"text-sm text-600 mr-3"},"Connected",-1)),f.value?y("",!0):(i(),d("i",Jo)),f.value?y("",!0):(i(),d("span",Qo,"Disconnected"))]),v(E(B),{icon:"pi pi-refresh",label:"Refresh All",class:"p-button-sm",loading:s.value,onClick:T},null,8,["loading"])])]),n.value?(i(),d("div",es,[v(E(Ke)),m[7]||(m[7]=a("span",{class:"ml-3 text-600"},"Loading crawler data...",-1))])):r.value?(i(),d("div",ts,[v(E(Ze),{severity:"error",closable:!1},{default:P(()=>[a("div",ns,[m[9]||(m[9]=a("i",{class:"pi pi-exclamation-triangle mr-2"},null,-1)),a("div",null,[m[8]||(m[8]=a("div",{class:"font-medium"},"Failed to load crawler data",-1)),a("div",os,w(r.value),1)])])]),_:1}),a("div",ss,[v(E(B),{icon:"pi pi-refresh",label:"Retry",onClick:j})])])):o.value.length===0?(i(),d("div",rs,m[10]||(m[10]=[at('<div class="flex align-items-center justify-content-center p-6 border-2 border-dashed surface-border border-round" data-v-5ba5f8a3><div class="text-center" data-v-5ba5f8a3><i class="pi pi-search text-4xl text-400 mb-3" data-v-5ba5f8a3></i><div class="text-900 font-medium mb-2" data-v-5ba5f8a3>No crawlers configured</div><div class="text-600" data-v-5ba5f8a3>Configure crawler instances in application.properties to get started</div></div></div>',1)]))):(i(),d("div",as,[(i(!0),d(F,null,Ie(o.value,k=>(i(),d("div",{key:k.configuration.id,class:"col-12 md:col-6 lg:col-4"},[v(Do,{crawler:k.configuration,health:k.health,onHealthUpdated:se},null,8,["crawler","health"])]))),128))])),c.value?(i(),d("div",is,[a("small",ls," Last updated: "+w(b.value),1)])):y("",!0)])]))}}),ds=he(cs,[["__scopeId","data-v-5ba5f8a3"]]),us=it({history:lt("/"),routes:[{path:"/",name:"dashboard",component:ds},{path:"/crawler/:id",name:"crawler-detail",component:()=>On(()=>import("./CrawlerDetail-CBjj3pSt.js"),__vite__mapDeps([0,1,2,3])),props:!0}]}),W=ct(jn);W.use(dt());W.use(us);W.use(Ct);W.use(kt);W.use(St);W.mount("#app");export{me as A,To as H,he as _,Ze as a,We as b,Ke as s,Ae as u};
