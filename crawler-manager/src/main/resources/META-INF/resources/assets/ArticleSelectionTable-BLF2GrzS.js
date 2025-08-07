import{B as O,j as X,o as ke,D as _e,l as G,g as I,q as ce,a2 as Ae,a0 as Ce,i as ze}from"./primevue-COZmI4QV.js";import{s as ue}from"./index-CMl7L9aA.js";import{h as x,f as c,c as ee,m as D,M as ve,r as V,B as k,w as M,d as $,s as g,N as i,i as t,e as _,v as p,t as w,A as Ie,k as Pe,o as Re,P as Ve,p as E,j as De,F as Le,y as Te,H as P,z as W}from"./vendor-Mr6oR_SG.js";import{s as Fe}from"./index-RJ29EJqU.js";import{s as U}from"./index-CdxwA4cm.js";import{d as Ue}from"./debounce-BMmXVQ06.js";import{_ as me}from"./index-DZVWbUD3.js";import{s as R}from"./index-Dp75atdH.js";import{s as de}from"./index-BQov9KEr.js";var je=`
    .p-inputgroup,
    .p-inputgroup .p-iconfield,
    .p-inputgroup .p-floatlabel,
    .p-inputgroup .p-iftalabel {
        display: flex;
        align-items: stretch;
        width: 100%;
    }

    .p-inputgroup .p-inputtext,
    .p-inputgroup .p-inputwrapper {
        flex: 1 1 auto;
        width: 1%;
    }

    .p-inputgroupaddon {
        display: flex;
        align-items: center;
        justify-content: center;
        padding: dt('inputgroup.addon.padding');
        background: dt('inputgroup.addon.background');
        color: dt('inputgroup.addon.color');
        border-block-start: 1px solid dt('inputgroup.addon.border.color');
        border-block-end: 1px solid dt('inputgroup.addon.border.color');
        min-width: dt('inputgroup.addon.min.width');
    }

    .p-inputgroupaddon:first-child,
    .p-inputgroupaddon + .p-inputgroupaddon {
        border-inline-start: 1px solid dt('inputgroup.addon.border.color');
    }

    .p-inputgroupaddon:last-child {
        border-inline-end: 1px solid dt('inputgroup.addon.border.color');
    }

    .p-inputgroupaddon:has(.p-button) {
        padding: 0;
        overflow: hidden;
    }

    .p-inputgroupaddon .p-button {
        border-radius: 0;
    }

    .p-inputgroup > .p-component,
    .p-inputgroup > .p-inputwrapper > .p-component,
    .p-inputgroup > .p-iconfield > .p-component,
    .p-inputgroup > .p-floatlabel > .p-component,
    .p-inputgroup > .p-floatlabel > .p-inputwrapper > .p-component,
    .p-inputgroup > .p-iftalabel > .p-component,
    .p-inputgroup > .p-iftalabel > .p-inputwrapper > .p-component {
        border-radius: 0;
        margin: 0;
    }

    .p-inputgroupaddon:first-child,
    .p-inputgroup > .p-component:first-child,
    .p-inputgroup > .p-inputwrapper:first-child > .p-component,
    .p-inputgroup > .p-iconfield:first-child > .p-component,
    .p-inputgroup > .p-floatlabel:first-child > .p-component,
    .p-inputgroup > .p-floatlabel:first-child > .p-inputwrapper > .p-component,
    .p-inputgroup > .p-iftalabel:first-child > .p-component,
    .p-inputgroup > .p-iftalabel:first-child > .p-inputwrapper > .p-component {
        border-start-start-radius: dt('inputgroup.addon.border.radius');
        border-end-start-radius: dt('inputgroup.addon.border.radius');
    }

    .p-inputgroupaddon:last-child,
    .p-inputgroup > .p-component:last-child,
    .p-inputgroup > .p-inputwrapper:last-child > .p-component,
    .p-inputgroup > .p-iconfield:last-child > .p-component,
    .p-inputgroup > .p-floatlabel:last-child > .p-component,
    .p-inputgroup > .p-floatlabel:last-child > .p-inputwrapper > .p-component,
    .p-inputgroup > .p-iftalabel:last-child > .p-component,
    .p-inputgroup > .p-iftalabel:last-child > .p-inputwrapper > .p-component {
        border-start-end-radius: dt('inputgroup.addon.border.radius');
        border-end-end-radius: dt('inputgroup.addon.border.radius');
    }

    .p-inputgroup .p-component:focus,
    .p-inputgroup .p-component.p-focus,
    .p-inputgroup .p-inputwrapper-focus,
    .p-inputgroup .p-component:focus ~ label,
    .p-inputgroup .p-component.p-focus ~ label,
    .p-inputgroup .p-inputwrapper-focus ~ label {
        z-index: 1;
    }

    .p-inputgroup > .p-button:not(.p-button-icon-only) {
        width: auto;
    }

    .p-inputgroup .p-iconfield + .p-iconfield .p-inputtext {
        border-inline-start: 0;
    }
`,Ee={root:"p-inputgroup"},Be=O.extend({name:"inputgroup",style:je,classes:Ee}),Ne={name:"BaseInputGroup",extends:X,style:Be,provide:function(){return{$pcInputGroup:this,$parentInstance:this}}},he={name:"InputGroup",extends:Ne,inheritAttrs:!1};function He(e,a,d,b,o,s){return c(),x("div",D({class:e.cx("root")},e.ptmi("root")),[ee(e.$slots,"default")],16)}he.render=He;var We={root:"p-inputgroupaddon"},Ge=O.extend({name:"inputgroupaddon",classes:We}),Me={name:"BaseInputGroupAddon",extends:X,style:Ge,provide:function(){return{$pcInputGroupAddon:this,$parentInstance:this}}},fe={name:"InputGroupAddon",extends:Me,inheritAttrs:!1};function Oe(e,a,d,b,o,s){return c(),x("div",D({class:e.cx("root")},e.ptmi("root")),[ee(e.$slots,"default")],16)}fe.render=Oe;const Xe={class:"grid"},qe={class:"col-12 md:col-6"},Ke={class:"field"},Je={class:"col-12 md:col-6"},Qe={class:"field"},Ye={class:"flex align-items-center justify-content-between w-full"},Ze={class:"col-12 md:col-6"},et={class:"field"},tt={class:"col-12 md:col-6"},nt={class:"field"},lt={class:"flex justify-content-between align-items-center mt-3"},at={class:"flex align-items-center gap-2"},rt={key:0,class:"text-600 text-sm"},it={key:0,class:"mt-3"},ot={class:"flex flex-wrap gap-2"},st=ve({__name:"ArticleFilter",props:{filters:{},authors:{}},emits:["filter-changed"],setup(e,{emit:a}){const d=e,b=a,o=V({...d.filters}),s=k(()=>[{label:"Drucker",value:"drucker"},{label:"Caspit",value:"caspit"}]);M(()=>d.filters,m=>{o.value={...m}},{deep:!0});const L=Ue(()=>{v()},300),v=()=>{b("filter-changed",{...o.value})},y=()=>{o.value={authorId:null,titleSearch:"",dateRange:null,crawlerSource:null},v()},C=k(()=>o.value.authorId!==null||o.value.titleSearch!==""||o.value.crawlerSource!==null||o.value.dateRange&&o.value.dateRange.length===2),T=k(()=>{let m=0;return o.value.authorId!==null&&m++,o.value.titleSearch!==""&&m++,o.value.crawlerSource!==null&&m++,o.value.dateRange&&o.value.dateRange.length===2&&m++,m}),q=m=>{const u=d.authors.find(h=>h.id===m);return(u==null?void 0:u.name)||"Unknown"},K=m=>{const u=s.value.find(h=>h.value===m);return(u==null?void 0:u.label)||m},A=m=>{if(!m||m.length!==2)return"";const u=m[0].toLocaleDateString(),h=m[1].toLocaleDateString();return`${u} - ${h}`},S=()=>{o.value.authorId=null,v()},z=()=>{o.value.titleSearch="",v()},J=()=>{o.value.crawlerSource=null,v()},Q=()=>{o.value.dateRange=null,v()};return(m,u)=>(c(),$(i(ke),{class:"article-filter"},{header:g(()=>u[4]||(u[4]=[t("div",{class:"flex align-items-center"},[t("i",{class:"pi pi-filter mr-2 text-primary"}),t("span",{class:"font-semibold"},"Filter Articles")],-1)])),default:g(()=>[t("div",Xe,[t("div",qe,[t("div",Ke,[u[6]||(u[6]=t("label",{for:"title-search",class:"font-semibold text-900"},"Search Title",-1)),p(i(he),null,{default:g(()=>[p(i(fe),null,{default:g(()=>u[5]||(u[5]=[t("i",{class:"pi pi-search"},null,-1)])),_:1,__:[5]}),p(i(_e),{id:"title-search",modelValue:o.value.titleSearch,"onUpdate:modelValue":u[0]||(u[0]=h=>o.value.titleSearch=h),placeholder:"Search in titles...",onInput:i(L)},null,8,["modelValue","onInput"])]),_:1})])]),t("div",Je,[t("div",Qe,[u[7]||(u[7]=t("label",{for:"author-filter",class:"font-semibold text-900"},"Author",-1)),p(i(ue),{id:"author-filter",modelValue:o.value.authorId,"onUpdate:modelValue":u[1]||(u[1]=h=>o.value.authorId=h),options:m.authors,optionLabel:"name",optionValue:"id",placeholder:"All Authors",showClear:"",class:"w-full",onChange:v},{option:g(({option:h})=>[t("div",Ye,[t("span",null,w(h.name),1),h.articleCount?(c(),$(i(G),{key:0,value:h.articleCount,severity:"secondary"},null,8,["value"])):_("",!0)])]),_:1},8,["modelValue","options"])])]),t("div",Ze,[t("div",et,[u[8]||(u[8]=t("label",{for:"crawler-source",class:"font-semibold text-900"},"Source",-1)),p(i(ue),{id:"crawler-source",modelValue:o.value.crawlerSource,"onUpdate:modelValue":u[2]||(u[2]=h=>o.value.crawlerSource=h),options:s.value,optionLabel:"label",optionValue:"value",placeholder:"All Sources",showClear:"",class:"w-full",onChange:v},null,8,["modelValue","options"])])]),t("div",tt,[t("div",nt,[u[9]||(u[9]=t("label",{for:"date-range",class:"font-semibold text-900"},"Date Range",-1)),p(i(Fe),{id:"date-range",modelValue:o.value.dateRange,"onUpdate:modelValue":u[3]||(u[3]=h=>o.value.dateRange=h),selectionMode:"range",dateFormat:"yy-mm-dd",showButtonBar:"",class:"w-full",onDateSelect:v},null,8,["modelValue"])])])]),t("div",lt,[t("div",at,[p(i(I),{icon:"pi pi-times",label:"Clear All",onClick:y,severity:"secondary",outlined:"",size:"small",disabled:!C.value},null,8,["disabled"]),C.value?(c(),x("span",rt,w(T.value)+" filter"+w(T.value>1?"s":"")+" active ",1)):_("",!0)])]),C.value?(c(),x("div",it,[t("div",ot,[o.value.authorId?(c(),$(i(U),{key:0,severity:"info",onClick:S,class:"cursor-pointer filter-tag"},{default:g(()=>[t("span",null,"Author: "+w(q(o.value.authorId)),1),u[10]||(u[10]=t("i",{class:"pi pi-times ml-2"},null,-1))]),_:1})):_("",!0),o.value.titleSearch?(c(),$(i(U),{key:1,severity:"info",onClick:z,class:"cursor-pointer filter-tag"},{default:g(()=>[t("span",null,'Title: "'+w(o.value.titleSearch)+'"',1),u[11]||(u[11]=t("i",{class:"pi pi-times ml-2"},null,-1))]),_:1})):_("",!0),o.value.crawlerSource?(c(),$(i(U),{key:2,severity:"info",onClick:J,class:"cursor-pointer filter-tag"},{default:g(()=>[t("span",null,"Source: "+w(K(o.value.crawlerSource)),1),u[12]||(u[12]=t("i",{class:"pi pi-times ml-2"},null,-1))]),_:1})):_("",!0),o.value.dateRange&&o.value.dateRange.length===2?(c(),$(i(U),{key:3,severity:"info",onClick:Q,class:"cursor-pointer filter-tag"},{default:g(()=>[t("span",null,"Date: "+w(A(o.value.dateRange)),1),u[13]||(u[13]=t("i",{class:"pi pi-times ml-2"},null,-1))]),_:1})):_("",!0)])])):_("",!0)]),_:1}))}}),bn=me(st,[["__scopeId","data-v-073e4a79"]]);var ut=`
    .p-avatar {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: dt('avatar.width');
        height: dt('avatar.height');
        font-size: dt('avatar.font.size');
        background: dt('avatar.background');
        color: dt('avatar.color');
        border-radius: dt('avatar.border.radius');
    }

    .p-avatar-image {
        background: transparent;
    }

    .p-avatar-circle {
        border-radius: 50%;
    }

    .p-avatar-circle img {
        border-radius: 50%;
    }

    .p-avatar-icon {
        font-size: dt('avatar.icon.size');
        width: dt('avatar.icon.size');
        height: dt('avatar.icon.size');
    }

    .p-avatar img {
        width: 100%;
        height: 100%;
    }

    .p-avatar-lg {
        width: dt('avatar.lg.width');
        height: dt('avatar.lg.width');
        font-size: dt('avatar.lg.font.size');
    }

    .p-avatar-lg .p-avatar-icon {
        font-size: dt('avatar.lg.icon.size');
        width: dt('avatar.lg.icon.size');
        height: dt('avatar.lg.icon.size');
    }

    .p-avatar-xl {
        width: dt('avatar.xl.width');
        height: dt('avatar.xl.width');
        font-size: dt('avatar.xl.font.size');
    }

    .p-avatar-xl .p-avatar-icon {
        font-size: dt('avatar.xl.icon.size');
        width: dt('avatar.xl.icon.size');
        height: dt('avatar.xl.icon.size');
    }

    .p-avatar-group {
        display: flex;
        align-items: center;
    }

    .p-avatar-group .p-avatar + .p-avatar {
        margin-inline-start: dt('avatar.group.offset');
    }

    .p-avatar-group .p-avatar {
        border: 2px solid dt('avatar.group.border.color');
    }

    .p-avatar-group .p-avatar-lg + .p-avatar-lg {
        margin-inline-start: dt('avatar.lg.group.offset');
    }

    .p-avatar-group .p-avatar-xl + .p-avatar-xl {
        margin-inline-start: dt('avatar.xl.group.offset');
    }
`,dt={root:function(a){var d=a.props;return["p-avatar p-component",{"p-avatar-image":d.image!=null,"p-avatar-circle":d.shape==="circle","p-avatar-lg":d.size==="large","p-avatar-xl":d.size==="xlarge"}]},label:"p-avatar-label",icon:"p-avatar-icon"},pt=O.extend({name:"avatar",style:ut,classes:dt}),ct={name:"BaseAvatar",extends:X,props:{label:{type:String,default:null},icon:{type:String,default:null},image:{type:String,default:null},size:{type:String,default:"normal"},shape:{type:String,default:"square"},ariaLabelledby:{type:String,default:null},ariaLabel:{type:String,default:null}},style:pt,provide:function(){return{$pcAvatar:this,$parentInstance:this}}};function N(e){"@babel/helpers - typeof";return N=typeof Symbol=="function"&&typeof Symbol.iterator=="symbol"?function(a){return typeof a}:function(a){return a&&typeof Symbol=="function"&&a.constructor===Symbol&&a!==Symbol.prototype?"symbol":typeof a},N(e)}function pe(e,a,d){return(a=vt(a))in e?Object.defineProperty(e,a,{value:d,enumerable:!0,configurable:!0,writable:!0}):e[a]=d,e}function vt(e){var a=mt(e,"string");return N(a)=="symbol"?a:a+""}function mt(e,a){if(N(e)!="object"||!e)return e;var d=e[Symbol.toPrimitive];if(d!==void 0){var b=d.call(e,a);if(N(b)!="object")return b;throw new TypeError("@@toPrimitive must return a primitive value.")}return(a==="string"?String:Number)(e)}var ge={name:"Avatar",extends:ct,inheritAttrs:!1,emits:["error"],methods:{onError:function(a){this.$emit("error",a)}},computed:{dataP:function(){return ce(pe(pe({},this.shape,this.shape),this.size,this.size))}}},ht=["aria-labelledby","aria-label","data-p"],ft=["data-p"],gt=["data-p"],bt=["src","alt","data-p"];function yt(e,a,d,b,o,s){return c(),x("div",D({class:e.cx("root"),"aria-labelledby":e.ariaLabelledby,"aria-label":e.ariaLabel},e.ptmi("root"),{"data-p":s.dataP}),[ee(e.$slots,"default",{},function(){return[e.label?(c(),x("span",D({key:0,class:e.cx("label")},e.ptm("label"),{"data-p":s.dataP}),w(e.label),17,ft)):e.$slots.icon?(c(),$(Pe(e.$slots.icon),{key:1,class:Ie(e.cx("icon"))},null,8,["class"])):e.icon?(c(),x("span",D({key:2,class:[e.cx("icon"),e.icon]},e.ptm("icon"),{"data-p":s.dataP}),null,16,gt)):e.image?(c(),x("img",D({key:3,src:e.image,alt:e.ariaLabel,onError:a[0]||(a[0]=function(){return s.onError&&s.onError.apply(s,arguments)})},e.ptm("image"),{"data-p":s.dataP}),null,16,bt)):_("",!0)]})],16,ht)}ge.render=yt;var wt=`
    .p-skeleton {
        display: block;
        overflow: hidden;
        background: dt('skeleton.background');
        border-radius: dt('skeleton.border.radius');
    }

    .p-skeleton::after {
        content: '';
        animation: p-skeleton-animation 1.2s infinite;
        height: 100%;
        left: 0;
        position: absolute;
        right: 0;
        top: 0;
        transform: translateX(-100%);
        z-index: 1;
        background: linear-gradient(90deg, rgba(255, 255, 255, 0), dt('skeleton.animation.background'), rgba(255, 255, 255, 0));
    }

    [dir='rtl'] .p-skeleton::after {
        animation-name: p-skeleton-animation-rtl;
    }

    .p-skeleton-circle {
        border-radius: 50%;
    }

    .p-skeleton-animation-none::after {
        animation: none;
    }

    @keyframes p-skeleton-animation {
        from {
            transform: translateX(-100%);
        }
        to {
            transform: translateX(100%);
        }
    }

    @keyframes p-skeleton-animation-rtl {
        from {
            transform: translateX(100%);
        }
        to {
            transform: translateX(-100%);
        }
    }
`,St={root:{position:"relative"}},xt={root:function(a){var d=a.props;return["p-skeleton p-component",{"p-skeleton-circle":d.shape==="circle","p-skeleton-animation-none":d.animation==="none"}]}},$t=O.extend({name:"skeleton",style:wt,classes:xt,inlineStyles:St}),kt={name:"BaseSkeleton",extends:X,props:{shape:{type:String,default:"rectangle"},size:{type:String,default:null},width:{type:String,default:"100%"},height:{type:String,default:"1rem"},borderRadius:{type:String,default:null},animation:{type:String,default:"wave"}},style:$t,provide:function(){return{$pcSkeleton:this,$parentInstance:this}}};function H(e){"@babel/helpers - typeof";return H=typeof Symbol=="function"&&typeof Symbol.iterator=="symbol"?function(a){return typeof a}:function(a){return a&&typeof Symbol=="function"&&a.constructor===Symbol&&a!==Symbol.prototype?"symbol":typeof a},H(e)}function _t(e,a,d){return(a=At(a))in e?Object.defineProperty(e,a,{value:d,enumerable:!0,configurable:!0,writable:!0}):e[a]=d,e}function At(e){var a=Ct(e,"string");return H(a)=="symbol"?a:a+""}function Ct(e,a){if(H(e)!="object"||!e)return e;var d=e[Symbol.toPrimitive];if(d!==void 0){var b=d.call(e,a);if(H(b)!="object")return b;throw new TypeError("@@toPrimitive must return a primitive value.")}return(a==="string"?String:Number)(e)}var B={name:"Skeleton",extends:kt,inheritAttrs:!1,computed:{containerStyle:function(){return this.size?{width:this.size,height:this.size,borderRadius:this.borderRadius}:{width:this.width,height:this.height,borderRadius:this.borderRadius}},dataP:function(){return ce(_t({},this.shape,this.shape))}}},zt=["data-p"];function It(e,a,d,b,o,s){return c(),x("div",D({class:e.cx("root"),style:[e.sx("root"),s.containerStyle],"aria-hidden":"true"},e.ptmi("root"),{"data-p":s.dataP}),null,16,zt)}B.render=It;const Pt={class:"article-selection-table-container"},Rt={class:"table-controls-header"},Vt={class:"flex align-items-center justify-content-between w-full"},Dt={class:"flex align-items-center gap-2"},Lt={class:"flex align-items-center gap-2 table-action-controls"},Tt={class:"table-content-area"},Ft={key:0,class:"loading-state"},Ut={class:"flex align-items-center"},jt={class:"flex-1"},Et={key:1,class:"empty-state"},Bt={class:"text-center p-6"},Nt={class:"text-center",role:"columnheader","aria-label":"Select all articles"},Ht={class:"article-title",role:"cell"},Wt=["title","aria-label"],Gt={class:"flex align-items-center gap-2"},Mt=["title","aria-label"],Ot={class:"flex align-items-center gap-2",role:"cell"},Xt={class:"text-center",role:"cell"},qt=["aria-label"],Kt={class:"text-600"},Jt={class:"text-center",role:"cell"},Qt={class:"text-center",role:"cell"},Yt={class:"flex justify-content-center",role:"cell"},Zt={key:0,class:"preview-content"},en={class:"preview-meta"},tn={class:"meta-item"},nn={class:"meta-item"},ln={class:"meta-item"},an={class:"meta-item"},rn=["href"],on={class:"preview-text"},sn=ve({__name:"ArticleSelectionTable",props:{articles:{},selectedArticles:{},loading:{type:Boolean,default:!1}},emits:["selection-changed","refresh-articles"],setup(e,{emit:a}){const d=e,b=a,o=V(!1),s=V([...d.selectedArticles]),L=V(!1),v=V(null),y=V(new Set(d.selectedArticles.map(n=>n.id))),C=V(window.innerHeight),T=k(()=>d.articles.length>100),q=k(()=>{const n=C.value-300;return`${Math.max(400,Math.min(800,n))}px`}),K=k(()=>T.value?{itemSize:73,showLoader:!0,delay:150,lazy:!0,numToleratedItems:10}:void 0),A=k(()=>{const n=C.value<768,l=C.value<1024;return{selection:{width:n?"50px":"60px",minWidth:n?"50px":"60px"},title:{minWidth:n?"200px":l?"250px":"300px",width:n?"40%":l?"35%":"30%"},author:{minWidth:n?"120px":l?"150px":"180px",width:n?"150px":l?"180px":"200px"},date:{minWidth:n?"100px":l?"120px":"140px",width:n?"120px":l?"140px":"160px"},source:{minWidth:n?"80px":l?"100px":"120px",width:n?"100px":l?"120px":"140px"},predictions:{minWidth:n?"100px":l?"120px":"140px",width:n?"120px":l?"140px":"160px"},actions:{width:n?"60px":"80px",minWidth:n?"60px":"80px"}}}),S=k(()=>d.articles),z=k(()=>S.value.length>0&&S.value.every(n=>y.value.has(n.id))),J=k(()=>{const n=S.value.filter(l=>y.value.has(l.id)).length;return n>0&&n<S.value.length});M(()=>d.selectedArticles,n=>{s.value=[...n],y.value=new Set(n.map(l=>l.id))},{deep:!0}),M(()=>d.articles,n=>{s.value=n.filter(l=>y.value.has(l.id))},{deep:!0}),M(s,()=>{xe()},{deep:!0});const Q=n=>{s.value=[...n.value],y.value=new Set(n.value.map(l=>l.id)),b("selection-changed",[...s.value])},m=n=>{const l=n.data;if(y.value.has(l.id)){y.value.delete(l.id);const F=s.value.findIndex(r=>r.id===l.id);F>=0&&s.value.splice(F,1)}else y.value.add(l.id),s.value.push(l);b("selection-changed",[...s.value])},u=async()=>{o.value=!0;try{b("refresh-articles"),await new Promise(n=>setTimeout(n,500))}finally{o.value=!1}},h=()=>{s.value=[],y.value.clear(),b("selection-changed",[])},Y=n=>new Date(n).toLocaleDateString(),te=n=>new Date(n).toLocaleTimeString([],{hour:"2-digit",minute:"2-digit"}),ne=()=>{if(z.value)S.value.forEach(n=>{y.value.delete(n.id)}),s.value=s.value.filter(n=>y.value.has(n.id));else{S.value.forEach(f=>{y.value.add(f.id)});const n=new Set(s.value.map(f=>f.id)),l=S.value.filter(f=>!n.has(f.id));s.value=[...s.value,...l]}},be=n=>{v.value=n,L.value=!0},le=n=>n?s.value.some(l=>l.id===n.id):!1,ye=n=>{if(n)if(y.value.has(n.id)){y.value.delete(n.id);const l=s.value.findIndex(f=>f.id===n.id);l>=0&&s.value.splice(l,1)}else y.value.add(n.id),s.value.push(n)},we=n=>{const l=JSON.parse(localStorage.getItem("articleTableColumnWidths")||"{}");l[n.element.dataset.field||n.element.textContent]=n.element.style.width,localStorage.setItem("articleTableColumnWidths",JSON.stringify(l))},Se=$e(()=>{b("selection-changed",[...s.value])},100),xe=()=>{Se()},Z=()=>{C.value=window.innerHeight};Re(()=>{window.addEventListener("resize",Z),Z()}),Ve(()=>{window.removeEventListener("resize",Z)});function $e(n,l){let f;return function(...r){const j=()=>{clearTimeout(f),n(...r)};clearTimeout(f),f=setTimeout(j,l)}}return(n,l)=>{var F;const f=De("tooltip");return c(),x("div",Pt,[t("div",Rt,[t("div",Vt,[t("div",Dt,[l[4]||(l[4]=t("i",{class:"pi pi-table text-primary"},null,-1)),l[5]||(l[5]=t("span",{class:"font-semibold"},"Articles",-1)),p(i(G),{value:s.value.length,severity:s.value.length>0?"success":"info"},null,8,["value","severity"]),p(i(de),{label:`${s.value.length} of ${S.value.length} selected`},null,8,["label"])]),t("div",Lt,[E(p(i(I),{icon:"pi pi-refresh",label:"Refresh",size:"small",severity:"secondary",outlined:"",onClick:u,loading:o.value},null,8,["loading"]),[[f,"Refresh article list",void 0,{top:!0}]]),E(p(i(I),{icon:"pi pi-check-square",label:z.value?"Deselect All":"Select All",size:"small",severity:z.value?"danger":"primary",outlined:"",onClick:ne,disabled:S.value.length===0},null,8,["label","severity","disabled"]),[[f,z.value?"Deselect all articles":"Select all articles",void 0,{top:!0}]]),s.value.length>0?E((c(),$(i(I),{key:0,icon:"pi pi-times",label:"Clear Selection",size:"small",severity:"secondary",outlined:"",onClick:h},null,512)),[[f,"Clear current selection",void 0,{top:!0}]]):_("",!0)])])]),t("div",Tt,[n.loading?(c(),x("div",Ft,[(c(),x(Le,null,Te(5,r=>t("div",{key:r,class:"mb-3"},[t("div",Ut,[p(i(B),{shape:"circle",size:"2rem",class:"mr-3"}),t("div",jt,[p(i(B),{width:"80%",height:"1.2rem",class:"mb-2"}),p(i(B),{width:"60%",height:"1rem"})]),p(i(B),{width:"4rem",height:"1rem"})])])),64))])):!n.loading&&n.articles.length===0?(c(),x("div",Et,[t("div",Bt,[l[6]||(l[6]=t("div",{class:"mb-4"},[t("i",{class:"pi pi-inbox text-6xl text-400"})],-1)),l[7]||(l[7]=t("div",{class:"text-900 font-medium text-xl mb-2"},"No Articles Found",-1)),l[8]||(l[8]=t("div",{class:"text-600 mb-4"},"No articles match your current filter criteria. Try adjusting your filters or refresh the data.",-1)),p(i(I),{icon:"pi pi-refresh",label:"Refresh Articles",severity:"secondary",onClick:u})])])):(c(),$(i(Ce),{key:2,selection:s.value,"onUpdate:selection":l[0]||(l[0]=r=>s.value=r),value:S.value,dataKey:"id",paginator:!T.value,rows:T.value?0:25,rowsPerPageOptions:[10,25,50,100],selectionMode:"multiple",class:"article-selection-table",responsiveLayout:"scroll",stripedRows:"",showGridlines:"",scrollable:!0,scrollHeight:q.value,virtualScrollerOptions:K.value,resizableColumns:"",columnResizeMode:"expand",loading:n.loading,loadingIcon:"pi pi-spinner",onSelectionChange:Q,onRowClick:m,onColumnResizeEnd:we,role:"table","aria-label":`Article selection table with ${S.value.length} articles`},{default:g(()=>[p(i(R),{selectionMode:"multiple",style:P(A.value.selection),resizable:!1,frozen:"",alignFrozen:"left",headerClass:"selection-header"},{header:g(()=>[t("div",Nt,[E(p(i(Ae),{modelValue:z.value,indeterminate:J.value,"onUpdate:modelValue":ne,disabled:S.value.length===0,"aria-label":z.value?"Deselect all articles":"Select all articles"},null,8,["modelValue","indeterminate","disabled","aria-label"]),[[f,"Select/deselect all articles",void 0,{top:!0}]])])]),_:1},8,["style"]),p(i(R),{field:"title",header:"Title",sortable:"",style:P(A.value.title),class:"title-column",headerClass:"title-header"},{body:g(({data:r})=>[t("div",Ht,[t("div",{class:"font-medium text-900 mb-1 line-height-3 title-text",title:r.title,"aria-label":`Article title: ${r.title}`},w(r.title),9,Wt),t("div",Gt,[p(i(U),{label:"URL",severity:"secondary"}),t("small",{class:"text-600 url-text",title:r.url,"aria-label":`Article URL: ${r.url}`},w(r.url),9,Mt)])])]),_:1},8,["style"]),p(i(R),{field:"author.name",header:"Author",sortable:"",style:P(A.value.author),class:"author-column",headerClass:"author-header"},{body:g(({data:r})=>{var j,ae,re,ie,oe,se;return[t("div",Ot,[p(i(ge),{image:(j=r.author)==null?void 0:j.avatarUrl,label:((re=(ae=r.author)==null?void 0:ae.name)==null?void 0:re.charAt(0))||"U",size:"small",class:"author-avatar","aria-label":`Author avatar for ${((ie=r.author)==null?void 0:ie.name)||"Unknown"}`},null,8,["image","label","aria-label"]),p(i(de),{label:((oe=r.author)==null?void 0:oe.name)||"Unknown",class:"author-chip","aria-label":`Author: ${((se=r.author)==null?void 0:se.name)||"Unknown"}`},null,8,["label","aria-label"])])]}),_:1},8,["style"]),p(i(R),{field:"createdAt",header:"Scraped",sortable:"",style:P(A.value.date),class:"date-column",headerClass:"date-header"},{body:g(({data:r})=>[t("div",Xt,[t("div",{class:"font-medium text-900","aria-label":`Scraped on ${Y(r.createdAt)} at ${te(r.createdAt)}`},w(Y(r.createdAt)),9,qt),t("small",Kt,w(te(r.createdAt)),1)])]),_:1},8,["style"]),p(i(R),{field:"crawlerSource",header:"Source",sortable:"",style:P(A.value.source),class:"source-column",headerClass:"source-header"},{body:g(({data:r})=>[t("div",Jt,[p(i(G),{value:r.crawlerSource,severity:r.crawlerSource==="drucker"?"info":"warning","aria-label":`Crawler source: ${r.crawlerSource}`},null,8,["value","severity","aria-label"])])]),_:1},8,["style"]),p(i(R),{header:"Predictions",style:P(A.value.predictions),class:"predictions-column",headerClass:"predictions-header"},{body:g(({data:r})=>[t("div",Qt,[r.predictions&&r.predictions.length>0?(c(),$(i(G),{key:0,value:`${r.predictions.length} found`,severity:"success","aria-label":`${r.predictions.length} predictions found`},null,8,["value","aria-label"])):(c(),$(i(U),{key:1,value:"Not analyzed",severity:"secondary","aria-label":"Article not yet analyzed for predictions"}))])]),_:1},8,["style"]),p(i(R),{header:"Actions",style:P(A.value.actions),resizable:!1,frozen:"",alignFrozen:"right",class:"actions-column",headerClass:"actions-header"},{body:g(({data:r})=>[t("div",Yt,[E(p(i(I),{icon:"pi pi-eye",severity:"secondary",text:"",size:"small",onClick:j=>be(r),"aria-label":`Preview article: ${r.title}`},null,8,["onClick","aria-label"]),[[f,"Preview article",void 0,{top:!0}]])])]),_:1},8,["style"])]),_:1},8,["selection","value","paginator","rows","scrollHeight","virtualScrollerOptions","loading","aria-label"]))]),p(i(ze),{visible:L.value,"onUpdate:visible":l[3]||(l[3]=r=>L.value=r),header:(F=v.value)==null?void 0:F.title,modal:"",style:{width:"80vw",maxWidth:"800px"},class:"article-preview-dialog"},{footer:g(()=>[p(i(I),{label:"Close",onClick:l[1]||(l[1]=r=>L.value=!1),severity:"secondary"}),p(i(I),{label:le(v.value)?"Remove from Selection":"Add to Selection",onClick:l[2]||(l[2]=r=>ye(v.value)),severity:le(v.value)?"danger":"primary"},null,8,["label","severity"])]),default:g(()=>{var r;return[v.value?(c(),x("div",Zt,[t("div",en,[t("div",tn,[l[9]||(l[9]=t("strong",null,"Author:",-1)),W(" "+w(((r=v.value.author)==null?void 0:r.name)||"Unknown"),1)]),t("div",nn,[l[10]||(l[10]=t("strong",null,"Source:",-1)),W(" "+w(v.value.crawlerSource),1)]),t("div",ln,[l[11]||(l[11]=t("strong",null,"Scraped:",-1)),W(" "+w(Y(v.value.createdAt)),1)]),t("div",an,[l[13]||(l[13]=t("strong",null,"URL:",-1)),t("a",{href:v.value.url,target:"_blank",class:"article-link"},[W(w(v.value.url)+" ",1),l[12]||(l[12]=t("i",{class:"pi pi-external-link"},null,-1))],8,rn)])]),t("div",on,[l[14]||(l[14]=t("h4",null,"Article Content",-1)),t("p",null,w(v.value.text||"No content available"),1)])])):_("",!0)]}),_:1},8,["visible","header"])])}}}),yn=me(sn,[["__scopeId","data-v-05ce9ff1"]]);export{bn as A,yn as a,ge as s};
