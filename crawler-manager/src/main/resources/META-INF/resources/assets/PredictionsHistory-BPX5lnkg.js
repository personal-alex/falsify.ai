import{h as m,e as O,f as p,c as T,d as N,m as b,k as ie,t as g,q as X,j as $e,i as s,v as h,F as ne,z as de,y as ke,s as C,A as U,C as yt,l as bt,E as gt,p as q,M as Pe,B as ue,N as f,r as H,w as _e,o as Ye,Q as Ot}from"./vendor-4T9WLNsm.js";import{a as xe,_ as Ve}from"./index-CLecQaJA.js";import{B as we,X as It,j as Ne,q as re,Z as kt,a0 as wt,a1 as St,a2 as Ct,a3 as At,a4 as Lt,a5 as Ft,H as Dt,a6 as Tt,a7 as Mt,D as Xe,p as Et,a8 as $t,a9 as se,f as Pt,aa as xt,z as Vt,ab as Oe,u as Ie,ac as Nt,Y as Rt,d as Kt,ad as zt,v as jt,J as Gt,x as Le,S as Ut,w as le,I as _t,ae as Ht,G as Bt,af as Jt,ag as oe,l as he,g as x,N as qt,i as Fe,ah as Qt}from"./primevue-DG7TyHL6.js";import{s as Z}from"./index-DlCKuXYx.js";import{s as Te}from"./index-De4VPVzf.js";import{s as He}from"./index-DIdlnjo6.js";import{s as Wt}from"./index-DHcX-VK9.js";import"./index-BgkhjCKx.js";var Yt=`
    .p-chip {
        display: inline-flex;
        align-items: center;
        background: dt('chip.background');
        color: dt('chip.color');
        border-radius: dt('chip.border.radius');
        padding-block: dt('chip.padding.y');
        padding-inline: dt('chip.padding.x');
        gap: dt('chip.gap');
    }

    .p-chip-icon {
        color: dt('chip.icon.color');
        font-size: dt('chip.icon.font.size');
        width: dt('chip.icon.size');
        height: dt('chip.icon.size');
    }

    .p-chip-image {
        border-radius: 50%;
        width: dt('chip.image.width');
        height: dt('chip.image.height');
        margin-inline-start: calc(-1 * dt('chip.padding.y'));
    }

    .p-chip:has(.p-chip-remove-icon) {
        padding-inline-end: dt('chip.padding.y');
    }

    .p-chip:has(.p-chip-image) {
        padding-block-start: calc(dt('chip.padding.y') / 2);
        padding-block-end: calc(dt('chip.padding.y') / 2);
    }

    .p-chip-remove-icon {
        cursor: pointer;
        font-size: dt('chip.remove.icon.size');
        width: dt('chip.remove.icon.size');
        height: dt('chip.remove.icon.size');
        color: dt('chip.remove.icon.color');
        border-radius: 50%;
        transition:
            outline-color dt('chip.transition.duration'),
            box-shadow dt('chip.transition.duration');
        outline-color: transparent;
    }

    .p-chip-remove-icon:focus-visible {
        box-shadow: dt('chip.remove.icon.focus.ring.shadow');
        outline: dt('chip.remove.icon.focus.ring.width') dt('chip.remove.icon.focus.ring.style') dt('chip.remove.icon.focus.ring.color');
        outline-offset: dt('chip.remove.icon.focus.ring.offset');
    }
`,Xt={root:"p-chip p-component",image:"p-chip-image",icon:"p-chip-icon",label:"p-chip-label",removeIcon:"p-chip-remove-icon"},Zt=we.extend({name:"chip",style:Yt,classes:Xt}),ei={name:"BaseChip",extends:Ne,props:{label:{type:[String,Number],default:null},icon:{type:String,default:null},image:{type:String,default:null},removable:{type:Boolean,default:!1},removeIcon:{type:String,default:void 0}},style:Zt,provide:function(){return{$pcChip:this,$parentInstance:this}}},Re={name:"Chip",extends:ei,inheritAttrs:!1,emits:["remove"],data:function(){return{visible:!0}},methods:{onKeydown:function(t){(t.key==="Enter"||t.key==="Backspace")&&this.close(t)},close:function(t){this.visible=!1,this.$emit("remove",t)}},computed:{dataP:function(){return re({removable:this.removable})}},components:{TimesCircleIcon:It}},ti=["aria-label","data-p"],ii=["src"];function ni(e,t,i,o,c,n){return c.visible?(p(),m("div",b({key:0,class:e.cx("root"),"aria-label":e.label},e.ptmi("root"),{"data-p":n.dataP}),[T(e.$slots,"default",{},function(){return[e.image?(p(),m("img",b({key:0,src:e.image},e.ptm("image"),{class:e.cx("image")}),null,16,ii)):e.$slots.icon?(p(),N(ie(e.$slots.icon),b({key:1,class:e.cx("icon")},e.ptm("icon")),null,16,["class"])):e.icon?(p(),m("span",b({key:2,class:[e.cx("icon"),e.icon]},e.ptm("icon")),null,16)):O("",!0),e.label!==null?(p(),m("div",b({key:3,class:e.cx("label")},e.ptm("label")),g(e.label),17)):O("",!0)]}),e.removable?T(e.$slots,"removeicon",{key:0,removeCallback:n.close,keydownCallback:n.onKeydown},function(){return[(p(),N(ie(e.removeIcon?"span":"TimesCircleIcon"),b({class:[e.cx("removeIcon"),e.removeIcon],onClick:n.close,onKeydown:n.onKeydown},e.ptm("removeIcon")),null,16,["class","onClick","onKeydown"]))]}):O("",!0)],16,ti)):O("",!0)}Re.render=ni;var si=`
    .p-multiselect {
        display: inline-flex;
        cursor: pointer;
        position: relative;
        user-select: none;
        background: dt('multiselect.background');
        border: 1px solid dt('multiselect.border.color');
        transition:
            background dt('multiselect.transition.duration'),
            color dt('multiselect.transition.duration'),
            border-color dt('multiselect.transition.duration'),
            outline-color dt('multiselect.transition.duration'),
            box-shadow dt('multiselect.transition.duration');
        border-radius: dt('multiselect.border.radius');
        outline-color: transparent;
        box-shadow: dt('multiselect.shadow');
    }

    .p-multiselect:not(.p-disabled):hover {
        border-color: dt('multiselect.hover.border.color');
    }

    .p-multiselect:not(.p-disabled).p-focus {
        border-color: dt('multiselect.focus.border.color');
        box-shadow: dt('multiselect.focus.ring.shadow');
        outline: dt('multiselect.focus.ring.width') dt('multiselect.focus.ring.style') dt('multiselect.focus.ring.color');
        outline-offset: dt('multiselect.focus.ring.offset');
    }

    .p-multiselect.p-variant-filled {
        background: dt('multiselect.filled.background');
    }

    .p-multiselect.p-variant-filled:not(.p-disabled):hover {
        background: dt('multiselect.filled.hover.background');
    }

    .p-multiselect.p-variant-filled.p-focus {
        background: dt('multiselect.filled.focus.background');
    }

    .p-multiselect.p-invalid {
        border-color: dt('multiselect.invalid.border.color');
    }

    .p-multiselect.p-disabled {
        opacity: 1;
        background: dt('multiselect.disabled.background');
    }

    .p-multiselect-dropdown {
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
        background: transparent;
        color: dt('multiselect.dropdown.color');
        width: dt('multiselect.dropdown.width');
        border-start-end-radius: dt('multiselect.border.radius');
        border-end-end-radius: dt('multiselect.border.radius');
    }

    .p-multiselect-clear-icon {
        position: absolute;
        top: 50%;
        margin-top: -0.5rem;
        color: dt('multiselect.clear.icon.color');
        inset-inline-end: dt('multiselect.dropdown.width');
    }

    .p-multiselect-label-container {
        overflow: hidden;
        flex: 1 1 auto;
        cursor: pointer;
    }

    .p-multiselect-label {
        white-space: nowrap;
        cursor: pointer;
        overflow: hidden;
        text-overflow: ellipsis;
        padding: dt('multiselect.padding.y') dt('multiselect.padding.x');
        color: dt('multiselect.color');
    }

    .p-multiselect-display-chip .p-multiselect-label {
        display: flex;
        align-items: center;
        gap: calc(dt('multiselect.padding.y') / 2);
    }

    .p-multiselect-label.p-placeholder {
        color: dt('multiselect.placeholder.color');
    }

    .p-multiselect.p-invalid .p-multiselect-label.p-placeholder {
        color: dt('multiselect.invalid.placeholder.color');
    }

    .p-multiselect.p-disabled .p-multiselect-label {
        color: dt('multiselect.disabled.color');
    }

    .p-multiselect-label-empty {
        overflow: hidden;
        visibility: hidden;
    }

    .p-multiselect-overlay {
        position: absolute;
        top: 0;
        left: 0;
        background: dt('multiselect.overlay.background');
        color: dt('multiselect.overlay.color');
        border: 1px solid dt('multiselect.overlay.border.color');
        border-radius: dt('multiselect.overlay.border.radius');
        box-shadow: dt('multiselect.overlay.shadow');
        min-width: 100%;
    }

    .p-multiselect-header {
        display: flex;
        align-items: center;
        padding: dt('multiselect.list.header.padding');
    }

    .p-multiselect-header .p-checkbox {
        margin-inline-end: dt('multiselect.option.gap');
    }

    .p-multiselect-filter-container {
        flex: 1 1 auto;
    }

    .p-multiselect-filter {
        width: 100%;
    }

    .p-multiselect-list-container {
        overflow: auto;
    }

    .p-multiselect-list {
        margin: 0;
        padding: 0;
        list-style-type: none;
        padding: dt('multiselect.list.padding');
        display: flex;
        flex-direction: column;
        gap: dt('multiselect.list.gap');
    }

    .p-multiselect-option {
        cursor: pointer;
        font-weight: normal;
        white-space: nowrap;
        position: relative;
        overflow: hidden;
        display: flex;
        align-items: center;
        gap: dt('multiselect.option.gap');
        padding: dt('multiselect.option.padding');
        border: 0 none;
        color: dt('multiselect.option.color');
        background: transparent;
        transition:
            background dt('multiselect.transition.duration'),
            color dt('multiselect.transition.duration'),
            border-color dt('multiselect.transition.duration'),
            box-shadow dt('multiselect.transition.duration'),
            outline-color dt('multiselect.transition.duration');
        border-radius: dt('multiselect.option.border.radius');
    }

    .p-multiselect-option:not(.p-multiselect-option-selected):not(.p-disabled).p-focus {
        background: dt('multiselect.option.focus.background');
        color: dt('multiselect.option.focus.color');
    }

    .p-multiselect-option.p-multiselect-option-selected {
        background: dt('multiselect.option.selected.background');
        color: dt('multiselect.option.selected.color');
    }

    .p-multiselect-option.p-multiselect-option-selected.p-focus {
        background: dt('multiselect.option.selected.focus.background');
        color: dt('multiselect.option.selected.focus.color');
    }

    .p-multiselect-option-group {
        cursor: auto;
        margin: 0;
        padding: dt('multiselect.option.group.padding');
        background: dt('multiselect.option.group.background');
        color: dt('multiselect.option.group.color');
        font-weight: dt('multiselect.option.group.font.weight');
    }

    .p-multiselect-empty-message {
        padding: dt('multiselect.empty.message.padding');
    }

    .p-multiselect-label .p-chip {
        padding-block-start: calc(dt('multiselect.padding.y') / 2);
        padding-block-end: calc(dt('multiselect.padding.y') / 2);
        border-radius: dt('multiselect.chip.border.radius');
    }

    .p-multiselect-label:has(.p-chip) {
        padding: calc(dt('multiselect.padding.y') / 2) calc(dt('multiselect.padding.x') / 2);
    }

    .p-multiselect-fluid {
        display: flex;
        width: 100%;
    }

    .p-multiselect-sm .p-multiselect-label {
        font-size: dt('multiselect.sm.font.size');
        padding-block: dt('multiselect.sm.padding.y');
        padding-inline: dt('multiselect.sm.padding.x');
    }

    .p-multiselect-sm .p-multiselect-dropdown .p-icon {
        font-size: dt('multiselect.sm.font.size');
        width: dt('multiselect.sm.font.size');
        height: dt('multiselect.sm.font.size');
    }

    .p-multiselect-lg .p-multiselect-label {
        font-size: dt('multiselect.lg.font.size');
        padding-block: dt('multiselect.lg.padding.y');
        padding-inline: dt('multiselect.lg.padding.x');
    }

    .p-multiselect-lg .p-multiselect-dropdown .p-icon {
        font-size: dt('multiselect.lg.font.size');
        width: dt('multiselect.lg.font.size');
        height: dt('multiselect.lg.font.size');
    }
`,li={root:function(t){var i=t.props;return{position:i.appendTo==="self"?"relative":void 0}}},oi={root:function(t){var i=t.instance,o=t.props;return["p-multiselect p-component p-inputwrapper",{"p-multiselect-display-chip":o.display==="chip","p-disabled":o.disabled,"p-invalid":i.$invalid,"p-variant-filled":i.$variant==="filled","p-focus":i.focused,"p-inputwrapper-filled":i.$filled,"p-inputwrapper-focus":i.focused||i.overlayVisible,"p-multiselect-open":i.overlayVisible,"p-multiselect-fluid":i.$fluid,"p-multiselect-sm p-inputfield-sm":o.size==="small","p-multiselect-lg p-inputfield-lg":o.size==="large"}]},labelContainer:"p-multiselect-label-container",label:function(t){var i=t.instance,o=t.props;return["p-multiselect-label",{"p-placeholder":i.label===o.placeholder,"p-multiselect-label-empty":!o.placeholder&&!i.$filled}]},clearIcon:"p-multiselect-clear-icon",chipItem:"p-multiselect-chip-item",pcChip:"p-multiselect-chip",chipIcon:"p-multiselect-chip-icon",dropdown:"p-multiselect-dropdown",loadingIcon:"p-multiselect-loading-icon",dropdownIcon:"p-multiselect-dropdown-icon",overlay:"p-multiselect-overlay p-component",header:"p-multiselect-header",pcFilterContainer:"p-multiselect-filter-container",pcFilter:"p-multiselect-filter",listContainer:"p-multiselect-list-container",list:"p-multiselect-list",optionGroup:"p-multiselect-option-group",option:function(t){var i=t.instance,o=t.option,c=t.index,n=t.getItemOptions,I=t.props;return["p-multiselect-option",{"p-multiselect-option-selected":i.isSelected(o)&&I.highlightOnSelect,"p-focus":i.focusedOptionIndex===i.getOptionIndex(c,n),"p-disabled":i.isOptionDisabled(o)}]},emptyMessage:"p-multiselect-empty-message"},ri=we.extend({name:"multiselect",style:si,classes:oi,inlineStyles:li}),ai={name:"BaseMultiSelect",extends:$t,props:{options:Array,optionLabel:null,optionValue:null,optionDisabled:null,optionGroupLabel:null,optionGroupChildren:null,scrollHeight:{type:String,default:"14rem"},placeholder:String,inputId:{type:String,default:null},panelClass:{type:String,default:null},panelStyle:{type:null,default:null},overlayClass:{type:String,default:null},overlayStyle:{type:null,default:null},dataKey:null,showClear:{type:Boolean,default:!1},clearIcon:{type:String,default:void 0},resetFilterOnClear:{type:Boolean,default:!1},filter:Boolean,filterPlaceholder:String,filterLocale:String,filterMatchMode:{type:String,default:"contains"},filterFields:{type:Array,default:null},appendTo:{type:[String,Object],default:"body"},display:{type:String,default:"comma"},selectedItemsLabel:{type:String,default:null},maxSelectedLabels:{type:Number,default:null},selectionLimit:{type:Number,default:null},showToggleAll:{type:Boolean,default:!0},loading:{type:Boolean,default:!1},checkboxIcon:{type:String,default:void 0},dropdownIcon:{type:String,default:void 0},filterIcon:{type:String,default:void 0},loadingIcon:{type:String,default:void 0},removeTokenIcon:{type:String,default:void 0},chipIcon:{type:String,default:void 0},selectAll:{type:Boolean,default:null},resetFilterOnHide:{type:Boolean,default:!1},virtualScrollerOptions:{type:Object,default:null},autoOptionFocus:{type:Boolean,default:!1},autoFilterFocus:{type:Boolean,default:!1},focusOnHover:{type:Boolean,default:!0},highlightOnSelect:{type:Boolean,default:!1},filterMessage:{type:String,default:null},selectionMessage:{type:String,default:null},emptySelectionMessage:{type:String,default:null},emptyFilterMessage:{type:String,default:null},emptyMessage:{type:String,default:null},tabindex:{type:Number,default:0},ariaLabel:{type:String,default:null},ariaLabelledby:{type:String,default:null}},style:ri,provide:function(){return{$pcMultiSelect:this,$parentInstance:this}}};function fe(e){"@babel/helpers - typeof";return fe=typeof Symbol=="function"&&typeof Symbol.iterator=="symbol"?function(t){return typeof t}:function(t){return t&&typeof Symbol=="function"&&t.constructor===Symbol&&t!==Symbol.prototype?"symbol":typeof t},fe(e)}function Be(e,t){var i=Object.keys(e);if(Object.getOwnPropertySymbols){var o=Object.getOwnPropertySymbols(e);t&&(o=o.filter(function(c){return Object.getOwnPropertyDescriptor(e,c).enumerable})),i.push.apply(i,o)}return i}function Je(e){for(var t=1;t<arguments.length;t++){var i=arguments[t]!=null?arguments[t]:{};t%2?Be(Object(i),!0).forEach(function(o){te(e,o,i[o])}):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(i)):Be(Object(i)).forEach(function(o){Object.defineProperty(e,o,Object.getOwnPropertyDescriptor(i,o))})}return e}function te(e,t,i){return(t=di(t))in e?Object.defineProperty(e,t,{value:i,enumerable:!0,configurable:!0,writable:!0}):e[t]=i,e}function di(e){var t=ci(e,"string");return fe(t)=="symbol"?t:t+""}function ci(e,t){if(fe(e)!="object"||!e)return e;var i=e[Symbol.toPrimitive];if(i!==void 0){var o=i.call(e,t);if(fe(o)!="object")return o;throw new TypeError("@@toPrimitive must return a primitive value.")}return(t==="string"?String:Number)(e)}function qe(e){return fi(e)||hi(e)||pi(e)||ui()}function ui(){throw new TypeError(`Invalid attempt to spread non-iterable instance.
In order to be iterable, non-array objects must have a [Symbol.iterator]() method.`)}function pi(e,t){if(e){if(typeof e=="string")return Me(e,t);var i={}.toString.call(e).slice(8,-1);return i==="Object"&&e.constructor&&(i=e.constructor.name),i==="Map"||i==="Set"?Array.from(e):i==="Arguments"||/^(?:Ui|I)nt(?:8|16|32)(?:Clamped)?Array$/.test(i)?Me(e,t):void 0}}function hi(e){if(typeof Symbol<"u"&&e[Symbol.iterator]!=null||e["@@iterator"]!=null)return Array.from(e)}function fi(e){if(Array.isArray(e))return Me(e)}function Me(e,t){(t==null||t>e.length)&&(t=e.length);for(var i=0,o=Array(t);i<t;i++)o[i]=e[i];return o}var Ee={name:"MultiSelect",extends:ai,inheritAttrs:!1,emits:["change","focus","blur","before-show","before-hide","show","hide","filter","selectall-change"],inject:{$pcFluid:{default:null}},outsideClickListener:null,scrollHandler:null,resizeListener:null,overlay:null,list:null,virtualScroller:null,startRangeIndex:-1,searchTimeout:null,searchValue:"",selectOnFocus:!1,data:function(){return{clicked:!1,focused:!1,focusedOptionIndex:-1,filterValue:null,overlayVisible:!1}},watch:{options:function(){this.autoUpdateModel()}},mounted:function(){this.autoUpdateModel()},beforeUnmount:function(){this.unbindOutsideClickListener(),this.unbindResizeListener(),this.scrollHandler&&(this.scrollHandler.destroy(),this.scrollHandler=null),this.overlay&&(Le.clear(this.overlay),this.overlay=null)},methods:{getOptionIndex:function(t,i){return this.virtualScrollerDisabled?t:i&&i(t).index},getOptionLabel:function(t){return this.optionLabel?oe(t,this.optionLabel):t},getOptionValue:function(t){return this.optionValue?oe(t,this.optionValue):t},getOptionRenderKey:function(t,i){return this.dataKey?oe(t,this.dataKey):this.getOptionLabel(t)+"_".concat(i)},getHeaderCheckboxPTOptions:function(t){return this.ptm(t,{context:{selected:this.allSelected}})},getCheckboxPTOptions:function(t,i,o,c){return this.ptm(c,{context:{selected:this.isSelected(t),focused:this.focusedOptionIndex===this.getOptionIndex(o,i),disabled:this.isOptionDisabled(t)}})},isOptionDisabled:function(t){return this.maxSelectionLimitReached&&!this.isSelected(t)?!0:this.optionDisabled?oe(t,this.optionDisabled):!1},isOptionGroup:function(t){return this.optionGroupLabel&&t.optionGroup&&t.group},getOptionGroupLabel:function(t){return oe(t,this.optionGroupLabel)},getOptionGroupChildren:function(t){return oe(t,this.optionGroupChildren)},getAriaPosInset:function(t){var i=this;return(this.optionGroupLabel?t-this.visibleOptions.slice(0,t).filter(function(o){return i.isOptionGroup(o)}).length:t)+1},show:function(t){this.$emit("before-show"),this.overlayVisible=!0,this.focusedOptionIndex=this.focusedOptionIndex!==-1?this.focusedOptionIndex:this.autoOptionFocus?this.findFirstFocusedOptionIndex():this.findSelectedOptionIndex(),t&&le(this.$refs.focusInput)},hide:function(t){var i=this,o=function(){i.$emit("before-hide"),i.overlayVisible=!1,i.clicked=!1,i.focusedOptionIndex=-1,i.searchValue="",i.resetFilterOnHide&&(i.filterValue=null),t&&le(i.$refs.focusInput)};setTimeout(function(){o()},0)},onFocus:function(t){this.disabled||(this.focused=!0,this.overlayVisible&&(this.focusedOptionIndex=this.focusedOptionIndex!==-1?this.focusedOptionIndex:this.autoOptionFocus?this.findFirstFocusedOptionIndex():this.findSelectedOptionIndex(),!this.autoFilterFocus&&this.scrollInView(this.focusedOptionIndex)),this.$emit("focus",t))},onBlur:function(t){var i,o;this.clicked=!1,this.focused=!1,this.focusedOptionIndex=-1,this.searchValue="",this.$emit("blur",t),(i=(o=this.formField).onBlur)===null||i===void 0||i.call(o)},onKeyDown:function(t){var i=this;if(this.disabled){t.preventDefault();return}var o=t.metaKey||t.ctrlKey;switch(t.code){case"ArrowDown":this.onArrowDownKey(t);break;case"ArrowUp":this.onArrowUpKey(t);break;case"Home":this.onHomeKey(t);break;case"End":this.onEndKey(t);break;case"PageDown":this.onPageDownKey(t);break;case"PageUp":this.onPageUpKey(t);break;case"Enter":case"NumpadEnter":case"Space":this.onEnterKey(t);break;case"Escape":this.onEscapeKey(t);break;case"Tab":this.onTabKey(t);break;case"ShiftLeft":case"ShiftRight":this.onShiftKey(t);break;default:if(t.code==="KeyA"&&o){var c=this.visibleOptions.filter(function(n){return i.isValidOption(n)}).map(function(n){return i.getOptionValue(n)});this.updateModel(t,c),t.preventDefault();break}!o&&Jt(t.key)&&(!this.overlayVisible&&this.show(),this.searchOptions(t),t.preventDefault());break}this.clicked=!1},onContainerClick:function(t){this.disabled||this.loading||t.target.tagName==="INPUT"||t.target.getAttribute("data-pc-section")==="clearicon"||t.target.closest('[data-pc-section="clearicon"]')||((!this.overlay||!this.overlay.contains(t.target))&&(this.overlayVisible?this.hide(!0):this.show(!0)),this.clicked=!0)},onClearClick:function(t){this.updateModel(t,null),this.resetFilterOnClear&&(this.filterValue=null)},onFirstHiddenFocus:function(t){var i=t.relatedTarget===this.$refs.focusInput?Bt(this.overlay,':not([data-p-hidden-focusable="true"])'):this.$refs.focusInput;le(i)},onLastHiddenFocus:function(t){var i=t.relatedTarget===this.$refs.focusInput?Ht(this.overlay,':not([data-p-hidden-focusable="true"])'):this.$refs.focusInput;le(i)},onOptionSelect:function(t,i){var o=this,c=arguments.length>2&&arguments[2]!==void 0?arguments[2]:-1,n=arguments.length>3&&arguments[3]!==void 0?arguments[3]:!1;if(!(this.disabled||this.isOptionDisabled(i))){var I=this.isSelected(i),y=null,k=this.getOptionValue(i)!==""?this.getOptionValue(i):this.getOptionLabel(i);I?y=this.d_value.filter(function(V){return!Ie(V,k,o.equalityKey)}):y=[].concat(qe(this.d_value||[]),[k]),this.updateModel(t,y),c!==-1&&(this.focusedOptionIndex=c),n&&le(this.$refs.focusInput)}},onOptionMouseMove:function(t,i){this.focusOnHover&&this.changeFocusedOptionIndex(t,i)},onOptionSelectRange:function(t){var i=this,o=arguments.length>1&&arguments[1]!==void 0?arguments[1]:-1,c=arguments.length>2&&arguments[2]!==void 0?arguments[2]:-1;if(o===-1&&(o=this.findNearestSelectedOptionIndex(c,!0)),c===-1&&(c=this.findNearestSelectedOptionIndex(o)),o!==-1&&c!==-1){var n=Math.min(o,c),I=Math.max(o,c),y=this.visibleOptions.slice(n,I+1).filter(function(k){return i.isValidOption(k)}).map(function(k){return i.getOptionValue(k)});this.updateModel(t,y)}},onFilterChange:function(t){var i=t.target.value;this.filterValue=i,this.focusedOptionIndex=-1,this.$emit("filter",{originalEvent:t,value:i}),!this.virtualScrollerDisabled&&this.virtualScroller.scrollToIndex(0)},onFilterKeyDown:function(t){switch(t.code){case"ArrowDown":this.onArrowDownKey(t);break;case"ArrowUp":this.onArrowUpKey(t,!0);break;case"ArrowLeft":case"ArrowRight":this.onArrowLeftKey(t,!0);break;case"Home":this.onHomeKey(t,!0);break;case"End":this.onEndKey(t,!0);break;case"Enter":case"NumpadEnter":this.onEnterKey(t);break;case"Escape":this.onEscapeKey(t);break;case"Tab":this.onTabKey(t,!0);break}},onFilterBlur:function(){this.focusedOptionIndex=-1},onFilterUpdated:function(){this.overlayVisible&&this.alignOverlay()},onOverlayClick:function(t){_t.emit("overlay-click",{originalEvent:t,target:this.$el})},onOverlayKeyDown:function(t){switch(t.code){case"Escape":this.onEscapeKey(t);break}},onArrowDownKey:function(t){if(!this.overlayVisible)this.show();else{var i=this.focusedOptionIndex!==-1?this.findNextOptionIndex(this.focusedOptionIndex):this.clicked?this.findFirstOptionIndex():this.findFirstFocusedOptionIndex();t.shiftKey&&this.onOptionSelectRange(t,this.startRangeIndex,i),this.changeFocusedOptionIndex(t,i)}t.preventDefault()},onArrowUpKey:function(t){var i=arguments.length>1&&arguments[1]!==void 0?arguments[1]:!1;if(t.altKey&&!i)this.focusedOptionIndex!==-1&&this.onOptionSelect(t,this.visibleOptions[this.focusedOptionIndex]),this.overlayVisible&&this.hide(),t.preventDefault();else{var o=this.focusedOptionIndex!==-1?this.findPrevOptionIndex(this.focusedOptionIndex):this.clicked?this.findLastOptionIndex():this.findLastFocusedOptionIndex();t.shiftKey&&this.onOptionSelectRange(t,o,this.startRangeIndex),this.changeFocusedOptionIndex(t,o),!this.overlayVisible&&this.show(),t.preventDefault()}},onArrowLeftKey:function(t){var i=arguments.length>1&&arguments[1]!==void 0?arguments[1]:!1;i&&(this.focusedOptionIndex=-1)},onHomeKey:function(t){var i=arguments.length>1&&arguments[1]!==void 0?arguments[1]:!1;if(i){var o=t.currentTarget;t.shiftKey?o.setSelectionRange(0,t.target.selectionStart):(o.setSelectionRange(0,0),this.focusedOptionIndex=-1)}else{var c=t.metaKey||t.ctrlKey,n=this.findFirstOptionIndex();t.shiftKey&&c&&this.onOptionSelectRange(t,n,this.startRangeIndex),this.changeFocusedOptionIndex(t,n),!this.overlayVisible&&this.show()}t.preventDefault()},onEndKey:function(t){var i=arguments.length>1&&arguments[1]!==void 0?arguments[1]:!1;if(i){var o=t.currentTarget;if(t.shiftKey)o.setSelectionRange(t.target.selectionStart,o.value.length);else{var c=o.value.length;o.setSelectionRange(c,c),this.focusedOptionIndex=-1}}else{var n=t.metaKey||t.ctrlKey,I=this.findLastOptionIndex();t.shiftKey&&n&&this.onOptionSelectRange(t,this.startRangeIndex,I),this.changeFocusedOptionIndex(t,I),!this.overlayVisible&&this.show()}t.preventDefault()},onPageUpKey:function(t){this.scrollInView(0),t.preventDefault()},onPageDownKey:function(t){this.scrollInView(this.visibleOptions.length-1),t.preventDefault()},onEnterKey:function(t){this.overlayVisible?this.focusedOptionIndex!==-1&&(t.shiftKey?this.onOptionSelectRange(t,this.focusedOptionIndex):this.onOptionSelect(t,this.visibleOptions[this.focusedOptionIndex])):(this.focusedOptionIndex=-1,this.onArrowDownKey(t)),t.preventDefault()},onEscapeKey:function(t){this.overlayVisible&&(this.hide(!0),t.stopPropagation()),t.preventDefault()},onTabKey:function(t){var i=arguments.length>1&&arguments[1]!==void 0?arguments[1]:!1;i||(this.overlayVisible&&this.hasFocusableElements()?(le(t.shiftKey?this.$refs.lastHiddenFocusableElementOnOverlay:this.$refs.firstHiddenFocusableElementOnOverlay),t.preventDefault()):(this.focusedOptionIndex!==-1&&this.onOptionSelect(t,this.visibleOptions[this.focusedOptionIndex]),this.overlayVisible&&this.hide(this.filter)))},onShiftKey:function(){this.startRangeIndex=this.focusedOptionIndex},onOverlayEnter:function(t){Le.set("overlay",t,this.$primevue.config.zIndex.overlay),Ut(t,{position:"absolute",top:"0"}),this.alignOverlay(),this.scrollInView(),this.autoFilterFocus&&le(this.$refs.filterInput.$el),this.autoUpdateModel(),this.$attrSelector&&t.setAttribute(this.$attrSelector,"")},onOverlayAfterEnter:function(){this.bindOutsideClickListener(),this.bindScrollListener(),this.bindResizeListener(),this.$emit("show")},onOverlayLeave:function(){this.unbindOutsideClickListener(),this.unbindScrollListener(),this.unbindResizeListener(),this.$emit("hide"),this.overlay=null},onOverlayAfterLeave:function(t){Le.clear(t)},alignOverlay:function(){this.appendTo==="self"?zt(this.overlay,this.$el):(this.overlay.style.minWidth=jt(this.$el)+"px",Gt(this.overlay,this.$el))},bindOutsideClickListener:function(){var t=this;this.outsideClickListener||(this.outsideClickListener=function(i){t.overlayVisible&&t.isOutsideClicked(i)&&t.hide()},document.addEventListener("click",this.outsideClickListener,!0))},unbindOutsideClickListener:function(){this.outsideClickListener&&(document.removeEventListener("click",this.outsideClickListener,!0),this.outsideClickListener=null)},bindScrollListener:function(){var t=this;this.scrollHandler||(this.scrollHandler=new Kt(this.$refs.container,function(){t.overlayVisible&&t.hide()})),this.scrollHandler.bindScrollListener()},unbindScrollListener:function(){this.scrollHandler&&this.scrollHandler.unbindScrollListener()},bindResizeListener:function(){var t=this;this.resizeListener||(this.resizeListener=function(){t.overlayVisible&&!Rt()&&t.hide()},window.addEventListener("resize",this.resizeListener))},unbindResizeListener:function(){this.resizeListener&&(window.removeEventListener("resize",this.resizeListener),this.resizeListener=null)},isOutsideClicked:function(t){return!(this.$el.isSameNode(t.target)||this.$el.contains(t.target)||this.overlay&&this.overlay.contains(t.target))},getLabelByValue:function(t){var i=this,o=this.optionGroupLabel?this.flatOptions(this.options):this.options||[],c=o.find(function(n){return!i.isOptionGroup(n)&&Ie(i.getOptionValue(n)!==""?i.getOptionValue(n):i.getOptionLabel(n),t,i.equalityKey)});return c?this.getOptionLabel(c):null},getSelectedItemsLabel:function(){var t=/{(.*?)}/,i=this.selectedItemsLabel||this.$primevue.config.locale.selectionMessage;return t.test(i)?i.replace(i.match(t)[0],this.d_value.length+""):i},onToggleAll:function(t){var i=this;if(this.selectAll!==null)this.$emit("selectall-change",{originalEvent:t,checked:!this.allSelected});else{var o=this.allSelected?[]:this.visibleOptions.filter(function(c){return i.isValidOption(c)}).map(function(c){return i.getOptionValue(c)});this.updateModel(t,o)}},removeOption:function(t,i){var o=this;t.stopPropagation();var c=this.d_value.filter(function(n){return!Ie(n,i,o.equalityKey)});this.updateModel(t,c)},clearFilter:function(){this.filterValue=null},hasFocusableElements:function(){return Nt(this.overlay,':not([data-p-hidden-focusable="true"])').length>0},isOptionMatched:function(t){var i;return this.isValidOption(t)&&typeof this.getOptionLabel(t)=="string"&&((i=this.getOptionLabel(t))===null||i===void 0?void 0:i.toLocaleLowerCase(this.filterLocale).startsWith(this.searchValue.toLocaleLowerCase(this.filterLocale)))},isValidOption:function(t){return se(t)&&!(this.isOptionDisabled(t)||this.isOptionGroup(t))},isValidSelectedOption:function(t){return this.isValidOption(t)&&this.isSelected(t)},isEquals:function(t,i){return Ie(t,i,this.equalityKey)},isSelected:function(t){var i=this,o=this.getOptionValue(t)!==""?this.getOptionValue(t):this.getOptionLabel(t);return(this.d_value||[]).some(function(c){return i.isEquals(c,o)})},findFirstOptionIndex:function(){var t=this;return this.visibleOptions.findIndex(function(i){return t.isValidOption(i)})},findLastOptionIndex:function(){var t=this;return Oe(this.visibleOptions,function(i){return t.isValidOption(i)})},findNextOptionIndex:function(t){var i=this,o=t<this.visibleOptions.length-1?this.visibleOptions.slice(t+1).findIndex(function(c){return i.isValidOption(c)}):-1;return o>-1?o+t+1:t},findPrevOptionIndex:function(t){var i=this,o=t>0?Oe(this.visibleOptions.slice(0,t),function(c){return i.isValidOption(c)}):-1;return o>-1?o:t},findSelectedOptionIndex:function(){var t=this;if(this.$filled){for(var i=function(){var I=t.d_value[c],y=t.visibleOptions.findIndex(function(k){return t.isValidSelectedOption(k)&&t.isEquals(I,t.getOptionValue(k))});if(y>-1)return{v:y}},o,c=this.d_value.length-1;c>=0;c--)if(o=i(),o)return o.v}return-1},findFirstSelectedOptionIndex:function(){var t=this;return this.$filled?this.visibleOptions.findIndex(function(i){return t.isValidSelectedOption(i)}):-1},findLastSelectedOptionIndex:function(){var t=this;return this.$filled?Oe(this.visibleOptions,function(i){return t.isValidSelectedOption(i)}):-1},findNextSelectedOptionIndex:function(t){var i=this,o=this.$filled&&t<this.visibleOptions.length-1?this.visibleOptions.slice(t+1).findIndex(function(c){return i.isValidSelectedOption(c)}):-1;return o>-1?o+t+1:-1},findPrevSelectedOptionIndex:function(t){var i=this,o=this.$filled&&t>0?Oe(this.visibleOptions.slice(0,t),function(c){return i.isValidSelectedOption(c)}):-1;return o>-1?o:-1},findNearestSelectedOptionIndex:function(t){var i=arguments.length>1&&arguments[1]!==void 0?arguments[1]:!1,o=-1;return this.$filled&&(i?(o=this.findPrevSelectedOptionIndex(t),o=o===-1?this.findNextSelectedOptionIndex(t):o):(o=this.findNextSelectedOptionIndex(t),o=o===-1?this.findPrevSelectedOptionIndex(t):o)),o>-1?o:t},findFirstFocusedOptionIndex:function(){var t=this.findFirstSelectedOptionIndex();return t<0?this.findFirstOptionIndex():t},findLastFocusedOptionIndex:function(){var t=this.findSelectedOptionIndex();return t<0?this.findLastOptionIndex():t},searchOptions:function(t){var i=this;this.searchValue=(this.searchValue||"")+t.key;var o=-1;se(this.searchValue)&&(this.focusedOptionIndex!==-1?(o=this.visibleOptions.slice(this.focusedOptionIndex).findIndex(function(c){return i.isOptionMatched(c)}),o=o===-1?this.visibleOptions.slice(0,this.focusedOptionIndex).findIndex(function(c){return i.isOptionMatched(c)}):o+this.focusedOptionIndex):o=this.visibleOptions.findIndex(function(c){return i.isOptionMatched(c)}),o===-1&&this.focusedOptionIndex===-1&&(o=this.findFirstFocusedOptionIndex()),o!==-1&&this.changeFocusedOptionIndex(t,o)),this.searchTimeout&&clearTimeout(this.searchTimeout),this.searchTimeout=setTimeout(function(){i.searchValue="",i.searchTimeout=null},500)},changeFocusedOptionIndex:function(t,i){this.focusedOptionIndex!==i&&(this.focusedOptionIndex=i,this.scrollInView(),this.selectOnFocus&&this.onOptionSelect(t,this.visibleOptions[i]))},scrollInView:function(){var t=this,i=arguments.length>0&&arguments[0]!==void 0?arguments[0]:-1;this.$nextTick(function(){var o=i!==-1?"".concat(t.$id,"_").concat(i):t.focusedOptionId,c=Vt(t.list,'li[id="'.concat(o,'"]'));c?c.scrollIntoView&&c.scrollIntoView({block:"nearest",inline:"nearest"}):t.virtualScrollerDisabled||t.virtualScroller&&t.virtualScroller.scrollToIndex(i!==-1?i:t.focusedOptionIndex)})},autoUpdateModel:function(){if(this.autoOptionFocus&&(this.focusedOptionIndex=this.findFirstFocusedOptionIndex()),this.selectOnFocus&&this.autoOptionFocus&&!this.$filled){var t=this.getOptionValue(this.visibleOptions[this.focusedOptionIndex]);this.updateModel(null,[t])}},updateModel:function(t,i){this.writeValue(i,t),this.$emit("change",{originalEvent:t,value:i})},flatOptions:function(t){var i=this;return(t||[]).reduce(function(o,c,n){o.push({optionGroup:c,group:!0,index:n});var I=i.getOptionGroupChildren(c);return I&&I.forEach(function(y){return o.push(y)}),o},[])},overlayRef:function(t){this.overlay=t},listRef:function(t,i){this.list=t,i&&i(t)},virtualScrollerRef:function(t){this.virtualScroller=t}},computed:{visibleOptions:function(){var t=this,i=this.optionGroupLabel?this.flatOptions(this.options):this.options||[];if(this.filterValue){var o=xt.filter(i,this.searchFields,this.filterValue,this.filterMatchMode,this.filterLocale);if(this.optionGroupLabel){var c=this.options||[],n=[];return c.forEach(function(I){var y=t.getOptionGroupChildren(I),k=y.filter(function(V){return o.includes(V)});k.length>0&&n.push(Je(Je({},I),{},te({},typeof t.optionGroupChildren=="string"?t.optionGroupChildren:"items",qe(k))))}),this.flatOptions(n)}return o}return i},label:function(){var t;if(this.d_value&&this.d_value.length){if(se(this.maxSelectedLabels)&&this.d_value.length>this.maxSelectedLabels)return this.getSelectedItemsLabel();t="";for(var i=0;i<this.d_value.length;i++)i!==0&&(t+=", "),t+=this.getLabelByValue(this.d_value[i])}else t=this.placeholder;return t},chipSelectedItems:function(){return se(this.maxSelectedLabels)&&this.d_value&&this.d_value.length>this.maxSelectedLabels},allSelected:function(){var t=this;return this.selectAll!==null?this.selectAll:se(this.visibleOptions)&&this.visibleOptions.every(function(i){return t.isOptionGroup(i)||t.isOptionDisabled(i)||t.isSelected(i)})},hasSelectedOption:function(){return this.$filled},equalityKey:function(){return this.optionValue?null:this.dataKey},searchFields:function(){return this.filterFields||[this.optionLabel]},maxSelectionLimitReached:function(){return this.selectionLimit&&this.d_value&&this.d_value.length===this.selectionLimit},filterResultMessageText:function(){return se(this.visibleOptions)?this.filterMessageText.replaceAll("{0}",this.visibleOptions.length):this.emptyFilterMessageText},filterMessageText:function(){return this.filterMessage||this.$primevue.config.locale.searchMessage||""},emptyFilterMessageText:function(){return this.emptyFilterMessage||this.$primevue.config.locale.emptySearchMessage||this.$primevue.config.locale.emptyFilterMessage||""},emptyMessageText:function(){return this.emptyMessage||this.$primevue.config.locale.emptyMessage||""},selectionMessageText:function(){return this.selectionMessage||this.$primevue.config.locale.selectionMessage||""},emptySelectionMessageText:function(){return this.emptySelectionMessage||this.$primevue.config.locale.emptySelectionMessage||""},selectedMessageText:function(){return this.$filled?this.selectionMessageText.replaceAll("{0}",this.d_value.length):this.emptySelectionMessageText},focusedOptionId:function(){return this.focusedOptionIndex!==-1?"".concat(this.$id,"_").concat(this.focusedOptionIndex):null},ariaSetSize:function(){var t=this;return this.visibleOptions.filter(function(i){return!t.isOptionGroup(i)}).length},toggleAllAriaLabel:function(){return this.$primevue.config.locale.aria?this.$primevue.config.locale.aria[this.allSelected?"selectAll":"unselectAll"]:void 0},listAriaLabel:function(){return this.$primevue.config.locale.aria?this.$primevue.config.locale.aria.listLabel:void 0},virtualScrollerDisabled:function(){return!this.virtualScrollerOptions},hasFluid:function(){return Pt(this.fluid)?!!this.$pcFluid:this.fluid},isClearIconVisible:function(){return this.showClear&&this.d_value&&this.d_value.length&&this.d_value!=null&&se(this.options)},containerDataP:function(){return re(te({invalid:this.$invalid,disabled:this.disabled,focus:this.focused,fluid:this.$fluid,filled:this.$variant==="filled"},this.size,this.size))},labelDataP:function(){return re(te(te(te({placeholder:this.label===this.placeholder,clearable:this.showClear,disabled:this.disabled},this.size,this.size),"has-chip",this.display==="chip"&&this.d_value&&this.d_value.length&&(this.maxSelectedLabels?this.d_value.length<=this.maxSelectedLabels:!0)),"empty",!this.placeholder&&!this.$filled))},dropdownIconDataP:function(){return re(te({},this.size,this.size))},overlayDataP:function(){return re(te({},"portal-"+this.appendTo,"portal-"+this.appendTo))}},directives:{ripple:Et},components:{InputText:Xe,Checkbox:Mt,VirtualScroller:Tt,Portal:Dt,Chip:Re,IconField:Ft,InputIcon:Lt,TimesIcon:At,SearchIcon:Ct,ChevronDownIcon:St,SpinnerIcon:wt,CheckIcon:kt}};function me(e){"@babel/helpers - typeof";return me=typeof Symbol=="function"&&typeof Symbol.iterator=="symbol"?function(t){return typeof t}:function(t){return t&&typeof Symbol=="function"&&t.constructor===Symbol&&t!==Symbol.prototype?"symbol":typeof t},me(e)}function Qe(e,t,i){return(t=mi(t))in e?Object.defineProperty(e,t,{value:i,enumerable:!0,configurable:!0,writable:!0}):e[t]=i,e}function mi(e){var t=vi(e,"string");return me(t)=="symbol"?t:t+""}function vi(e,t){if(me(e)!="object"||!e)return e;var i=e[Symbol.toPrimitive];if(i!==void 0){var o=i.call(e,t);if(me(o)!="object")return o;throw new TypeError("@@toPrimitive must return a primitive value.")}return(t==="string"?String:Number)(e)}var yi=["data-p"],bi=["id","disabled","placeholder","tabindex","aria-label","aria-labelledby","aria-expanded","aria-controls","aria-activedescendant","aria-invalid"],gi=["data-p"],Oi={key:0},Ii=["data-p"],ki=["id","aria-label"],wi=["id"],Si=["id","aria-label","aria-selected","aria-disabled","aria-setsize","aria-posinset","onClick","onMousemove","data-p-selected","data-p-focused","data-p-disabled"];function Ci(e,t,i,o,c,n){var I=X("Chip"),y=X("SpinnerIcon"),k=X("Checkbox"),V=X("InputText"),_=X("SearchIcon"),M=X("InputIcon"),K=X("IconField"),B=X("VirtualScroller"),j=X("Portal"),E=$e("ripple");return p(),m("div",b({ref:"container",class:e.cx("root"),style:e.sx("root"),onClick:t[7]||(t[7]=function(){return n.onContainerClick&&n.onContainerClick.apply(n,arguments)}),"data-p":n.containerDataP},e.ptmi("root")),[s("div",b({class:"p-hidden-accessible"},e.ptm("hiddenInputContainer"),{"data-p-hidden-accessible":!0}),[s("input",b({ref:"focusInput",id:e.inputId,type:"text",readonly:"",disabled:e.disabled,placeholder:e.placeholder,tabindex:e.disabled?-1:e.tabindex,role:"combobox","aria-label":e.ariaLabel,"aria-labelledby":e.ariaLabelledby,"aria-haspopup":"listbox","aria-expanded":c.overlayVisible,"aria-controls":e.$id+"_list","aria-activedescendant":c.focused?n.focusedOptionId:void 0,"aria-invalid":e.invalid||void 0,onFocus:t[0]||(t[0]=function(){return n.onFocus&&n.onFocus.apply(n,arguments)}),onBlur:t[1]||(t[1]=function(){return n.onBlur&&n.onBlur.apply(n,arguments)}),onKeydown:t[2]||(t[2]=function(){return n.onKeyDown&&n.onKeyDown.apply(n,arguments)})},e.ptm("hiddenInput")),null,16,bi)],16),s("div",b({class:e.cx("labelContainer")},e.ptm("labelContainer")),[s("div",b({class:e.cx("label"),"data-p":n.labelDataP},e.ptm("label")),[T(e.$slots,"value",{value:e.d_value,placeholder:e.placeholder},function(){return[e.display==="comma"?(p(),m(ne,{key:0},[de(g(n.label||"empty"),1)],64)):e.display==="chip"?(p(),m(ne,{key:1},[n.chipSelectedItems?(p(),m("span",Oi,g(n.label),1)):(p(!0),m(ne,{key:1},ke(e.d_value,function(v){return p(),m("span",b({key:n.getLabelByValue(v),class:e.cx("chipItem")},{ref_for:!0},e.ptm("chipItem")),[T(e.$slots,"chip",{value:v,removeCallback:function(J){return n.removeOption(J,v)}},function(){return[h(I,{class:U(e.cx("pcChip")),label:n.getLabelByValue(v),removeIcon:e.chipIcon||e.removeTokenIcon,removable:"",unstyled:e.unstyled,onRemove:function(J){return n.removeOption(J,v)},pt:e.ptm("pcChip")},{removeicon:C(function(){return[T(e.$slots,e.$slots.chipicon?"chipicon":"removetokenicon",{class:U(e.cx("chipIcon")),item:v,removeCallback:function(J){return n.removeOption(J,v)}})]}),_:2},1032,["class","label","removeIcon","unstyled","onRemove","pt"])]})],16)}),128)),!e.d_value||e.d_value.length===0?(p(),m(ne,{key:2},[de(g(e.placeholder||"empty"),1)],64)):O("",!0)],64)):O("",!0)]})],16,gi)],16),n.isClearIconVisible?T(e.$slots,"clearicon",{key:0,class:U(e.cx("clearIcon")),clearCallback:n.onClearClick},function(){return[(p(),N(ie(e.clearIcon?"i":"TimesIcon"),b({ref:"clearIcon",class:[e.cx("clearIcon"),e.clearIcon],onClick:n.onClearClick},e.ptm("clearIcon"),{"data-pc-section":"clearicon"}),null,16,["class","onClick"]))]}):O("",!0),s("div",b({class:e.cx("dropdown")},e.ptm("dropdown")),[e.loading?T(e.$slots,"loadingicon",{key:0,class:U(e.cx("loadingIcon"))},function(){return[e.loadingIcon?(p(),m("span",b({key:0,class:[e.cx("loadingIcon"),"pi-spin",e.loadingIcon],"aria-hidden":"true"},e.ptm("loadingIcon")),null,16)):(p(),N(y,b({key:1,class:e.cx("loadingIcon"),spin:"","aria-hidden":"true"},e.ptm("loadingIcon")),null,16,["class"]))]}):T(e.$slots,"dropdownicon",{key:1,class:U(e.cx("dropdownIcon"))},function(){return[(p(),N(ie(e.dropdownIcon?"span":"ChevronDownIcon"),b({class:[e.cx("dropdownIcon"),e.dropdownIcon],"aria-hidden":"true","data-p":n.dropdownIconDataP},e.ptm("dropdownIcon")),null,16,["class","data-p"]))]})],16),h(j,{appendTo:e.appendTo},{default:C(function(){return[h(yt,b({name:"p-connected-overlay",onEnter:n.onOverlayEnter,onAfterEnter:n.onOverlayAfterEnter,onLeave:n.onOverlayLeave,onAfterLeave:n.onOverlayAfterLeave},e.ptm("transition")),{default:C(function(){return[c.overlayVisible?(p(),m("div",b({key:0,ref:n.overlayRef,style:[e.panelStyle,e.overlayStyle],class:[e.cx("overlay"),e.panelClass,e.overlayClass],onClick:t[5]||(t[5]=function(){return n.onOverlayClick&&n.onOverlayClick.apply(n,arguments)}),onKeydown:t[6]||(t[6]=function(){return n.onOverlayKeyDown&&n.onOverlayKeyDown.apply(n,arguments)}),"data-p":n.overlayDataP},e.ptm("overlay")),[s("span",b({ref:"firstHiddenFocusableElementOnOverlay",role:"presentation","aria-hidden":"true",class:"p-hidden-accessible p-hidden-focusable",tabindex:0,onFocus:t[3]||(t[3]=function(){return n.onFirstHiddenFocus&&n.onFirstHiddenFocus.apply(n,arguments)})},e.ptm("hiddenFirstFocusableEl"),{"data-p-hidden-accessible":!0,"data-p-hidden-focusable":!0}),null,16),T(e.$slots,"header",{value:e.d_value,options:n.visibleOptions}),e.showToggleAll&&e.selectionLimit==null||e.filter?(p(),m("div",b({key:0,class:e.cx("header")},e.ptm("header")),[e.showToggleAll&&e.selectionLimit==null?(p(),N(k,{key:0,modelValue:n.allSelected,binary:!0,disabled:e.disabled,variant:e.variant,"aria-label":n.toggleAllAriaLabel,onChange:n.onToggleAll,unstyled:e.unstyled,pt:n.getHeaderCheckboxPTOptions("pcHeaderCheckbox"),formControl:{novalidate:!0}},{icon:C(function(v){return[e.$slots.headercheckboxicon?(p(),N(ie(e.$slots.headercheckboxicon),{key:0,checked:v.checked,class:U(v.class)},null,8,["checked","class"])):v.checked?(p(),N(ie(e.checkboxIcon?"span":"CheckIcon"),b({key:1,class:[v.class,Qe({},e.checkboxIcon,v.checked)]},n.getHeaderCheckboxPTOptions("pcHeaderCheckbox.icon")),null,16,["class"])):O("",!0)]}),_:1},8,["modelValue","disabled","variant","aria-label","onChange","unstyled","pt"])):O("",!0),e.filter?(p(),N(K,{key:1,class:U(e.cx("pcFilterContainer")),unstyled:e.unstyled,pt:e.ptm("pcFilterContainer")},{default:C(function(){return[h(V,{ref:"filterInput",value:c.filterValue,onVnodeMounted:n.onFilterUpdated,onVnodeUpdated:n.onFilterUpdated,class:U(e.cx("pcFilter")),placeholder:e.filterPlaceholder,disabled:e.disabled,variant:e.variant,unstyled:e.unstyled,role:"searchbox",autocomplete:"off","aria-owns":e.$id+"_list","aria-activedescendant":n.focusedOptionId,onKeydown:n.onFilterKeyDown,onBlur:n.onFilterBlur,onInput:n.onFilterChange,pt:e.ptm("pcFilter"),formControl:{novalidate:!0}},null,8,["value","onVnodeMounted","onVnodeUpdated","class","placeholder","disabled","variant","unstyled","aria-owns","aria-activedescendant","onKeydown","onBlur","onInput","pt"]),h(M,{unstyled:e.unstyled,pt:e.ptm("pcFilterIconContainer")},{default:C(function(){return[T(e.$slots,"filtericon",{},function(){return[e.filterIcon?(p(),m("span",b({key:0,class:e.filterIcon},e.ptm("filterIcon")),null,16)):(p(),N(_,bt(b({key:1},e.ptm("filterIcon"))),null,16))]})]}),_:3},8,["unstyled","pt"])]}),_:3},8,["class","unstyled","pt"])):O("",!0),e.filter?(p(),m("span",b({key:2,role:"status","aria-live":"polite",class:"p-hidden-accessible"},e.ptm("hiddenFilterResult"),{"data-p-hidden-accessible":!0}),g(n.filterResultMessageText),17)):O("",!0)],16)):O("",!0),s("div",b({class:e.cx("listContainer"),style:{"max-height":n.virtualScrollerDisabled?e.scrollHeight:""}},e.ptm("listContainer")),[h(B,b({ref:n.virtualScrollerRef},e.virtualScrollerOptions,{items:n.visibleOptions,style:{height:e.scrollHeight},tabindex:-1,disabled:n.virtualScrollerDisabled,pt:e.ptm("virtualScroller")}),gt({content:C(function(v){var G=v.styleClass,J=v.contentRef,W=v.items,$=v.getItemOptions,pe=v.contentStyle,Y=v.itemSize;return[s("ul",b({ref:function(A){return n.listRef(A,J)},id:e.$id+"_list",class:[e.cx("list"),G],style:pe,role:"listbox","aria-multiselectable":"true","aria-label":n.listAriaLabel},e.ptm("list")),[(p(!0),m(ne,null,ke(W,function(F,A){return p(),m(ne,{key:n.getOptionRenderKey(F,n.getOptionIndex(A,$))},[n.isOptionGroup(F)?(p(),m("li",b({key:0,id:e.$id+"_"+n.getOptionIndex(A,$),style:{height:Y?Y+"px":void 0},class:e.cx("optionGroup"),role:"option"},{ref_for:!0},e.ptm("optionGroup")),[T(e.$slots,"optiongroup",{option:F.optionGroup,index:n.getOptionIndex(A,$)},function(){return[de(g(n.getOptionGroupLabel(F.optionGroup)),1)]})],16,wi)):q((p(),m("li",b({key:1,id:e.$id+"_"+n.getOptionIndex(A,$),style:{height:Y?Y+"px":void 0},class:e.cx("option",{option:F,index:A,getItemOptions:$}),role:"option","aria-label":n.getOptionLabel(F),"aria-selected":n.isSelected(F),"aria-disabled":n.isOptionDisabled(F),"aria-setsize":n.ariaSetSize,"aria-posinset":n.getAriaPosInset(n.getOptionIndex(A,$)),onClick:function(ee){return n.onOptionSelect(ee,F,n.getOptionIndex(A,$),!0)},onMousemove:function(ee){return n.onOptionMouseMove(ee,n.getOptionIndex(A,$))}},{ref_for:!0},n.getCheckboxPTOptions(F,$,A,"option"),{"data-p-selected":n.isSelected(F),"data-p-focused":c.focusedOptionIndex===n.getOptionIndex(A,$),"data-p-disabled":n.isOptionDisabled(F)}),[h(k,{defaultValue:n.isSelected(F),binary:!0,tabindex:-1,variant:e.variant,unstyled:e.unstyled,pt:n.getCheckboxPTOptions(F,$,A,"pcOptionCheckbox"),formControl:{novalidate:!0}},{icon:C(function(z){return[e.$slots.optioncheckboxicon||e.$slots.itemcheckboxicon?(p(),N(ie(e.$slots.optioncheckboxicon||e.$slots.itemcheckboxicon),{key:0,checked:z.checked,class:U(z.class)},null,8,["checked","class"])):z.checked?(p(),N(ie(e.checkboxIcon?"span":"CheckIcon"),b({key:1,class:[z.class,Qe({},e.checkboxIcon,z.checked)]},{ref_for:!0},n.getCheckboxPTOptions(F,$,A,"pcOptionCheckbox.icon")),null,16,["class"])):O("",!0)]}),_:2},1032,["defaultValue","variant","unstyled","pt"]),T(e.$slots,"option",{option:F,selected:n.isSelected(F),index:n.getOptionIndex(A,$)},function(){return[s("span",b({ref_for:!0},e.ptm("optionLabel")),g(n.getOptionLabel(F)),17)]})],16,Si)),[[E]])],64)}),128)),c.filterValue&&(!W||W&&W.length===0)?(p(),m("li",b({key:0,class:e.cx("emptyMessage"),role:"option"},e.ptm("emptyMessage")),[T(e.$slots,"emptyfilter",{},function(){return[de(g(n.emptyFilterMessageText),1)]})],16)):!e.options||e.options&&e.options.length===0?(p(),m("li",b({key:1,class:e.cx("emptyMessage"),role:"option"},e.ptm("emptyMessage")),[T(e.$slots,"empty",{},function(){return[de(g(n.emptyMessageText),1)]})],16)):O("",!0)],16,ki)]}),_:2},[e.$slots.loader?{name:"loader",fn:C(function(v){var G=v.options;return[T(e.$slots,"loader",{options:G})]}),key:"0"}:void 0]),1040,["items","style","disabled","pt"])],16),T(e.$slots,"footer",{value:e.d_value,options:n.visibleOptions}),!e.options||e.options&&e.options.length===0?(p(),m("span",b({key:1,role:"status","aria-live":"polite",class:"p-hidden-accessible"},e.ptm("hiddenEmptyMessage"),{"data-p-hidden-accessible":!0}),g(n.emptyMessageText),17)):O("",!0),s("span",b({role:"status","aria-live":"polite",class:"p-hidden-accessible"},e.ptm("hiddenSelectedMessage"),{"data-p-hidden-accessible":!0}),g(n.selectedMessageText),17),s("span",b({ref:"lastHiddenFocusableElementOnOverlay",role:"presentation","aria-hidden":"true",class:"p-hidden-accessible p-hidden-focusable",tabindex:0,onFocus:t[4]||(t[4]=function(){return n.onLastHiddenFocus&&n.onLastHiddenFocus.apply(n,arguments)})},e.ptm("hiddenLastFocusableEl"),{"data-p-hidden-accessible":!0,"data-p-hidden-focusable":!0}),null,16)],16,Ii)):O("",!0)]}),_:3},16,["onEnter","onAfterEnter","onLeave","onAfterLeave"])]}),_:3},8,["appendTo"])],16,yi)}Ee.render=Ci;var Ai=`
    .p-divider-horizontal {
        display: flex;
        width: 100%;
        position: relative;
        align-items: center;
        margin: dt('divider.horizontal.margin');
        padding: dt('divider.horizontal.padding');
    }

    .p-divider-horizontal:before {
        position: absolute;
        display: block;
        inset-block-start: 50%;
        inset-inline-start: 0;
        width: 100%;
        content: '';
        border-block-start: 1px solid dt('divider.border.color');
    }

    .p-divider-horizontal .p-divider-content {
        padding: dt('divider.horizontal.content.padding');
    }

    .p-divider-vertical {
        min-height: 100%;
        display: flex;
        position: relative;
        justify-content: center;
        margin: dt('divider.vertical.margin');
        padding: dt('divider.vertical.padding');
    }

    .p-divider-vertical:before {
        position: absolute;
        display: block;
        inset-block-start: 0;
        inset-inline-start: 50%;
        height: 100%;
        content: '';
        border-inline-start: 1px solid dt('divider.border.color');
    }

    .p-divider.p-divider-vertical .p-divider-content {
        padding: dt('divider.vertical.content.padding');
    }

    .p-divider-content {
        z-index: 1;
        background: dt('divider.content.background');
        color: dt('divider.content.color');
    }

    .p-divider-solid.p-divider-horizontal:before {
        border-block-start-style: solid;
    }

    .p-divider-solid.p-divider-vertical:before {
        border-inline-start-style: solid;
    }

    .p-divider-dashed.p-divider-horizontal:before {
        border-block-start-style: dashed;
    }

    .p-divider-dashed.p-divider-vertical:before {
        border-inline-start-style: dashed;
    }

    .p-divider-dotted.p-divider-horizontal:before {
        border-block-start-style: dotted;
    }

    .p-divider-dotted.p-divider-vertical:before {
        border-inline-start-style: dotted;
    }

    .p-divider-left:dir(rtl),
    .p-divider-right:dir(rtl) {
        flex-direction: row-reverse;
    }
`,Li={root:function(t){var i=t.props;return{justifyContent:i.layout==="horizontal"?i.align==="center"||i.align===null?"center":i.align==="left"?"flex-start":i.align==="right"?"flex-end":null:null,alignItems:i.layout==="vertical"?i.align==="center"||i.align===null?"center":i.align==="top"?"flex-start":i.align==="bottom"?"flex-end":null:null}}},Fi={root:function(t){var i=t.props;return["p-divider p-component","p-divider-"+i.layout,"p-divider-"+i.type,{"p-divider-left":i.layout==="horizontal"&&(!i.align||i.align==="left")},{"p-divider-center":i.layout==="horizontal"&&i.align==="center"},{"p-divider-right":i.layout==="horizontal"&&i.align==="right"},{"p-divider-top":i.layout==="vertical"&&i.align==="top"},{"p-divider-center":i.layout==="vertical"&&(!i.align||i.align==="center")},{"p-divider-bottom":i.layout==="vertical"&&i.align==="bottom"}]},content:"p-divider-content"},Di=we.extend({name:"divider",style:Ai,classes:Fi,inlineStyles:Li}),Ti={name:"BaseDivider",extends:Ne,props:{align:{type:String,default:null},layout:{type:String,default:"horizontal"},type:{type:String,default:"solid"}},style:Di,provide:function(){return{$pcDivider:this,$parentInstance:this}}};function ve(e){"@babel/helpers - typeof";return ve=typeof Symbol=="function"&&typeof Symbol.iterator=="symbol"?function(t){return typeof t}:function(t){return t&&typeof Symbol=="function"&&t.constructor===Symbol&&t!==Symbol.prototype?"symbol":typeof t},ve(e)}function De(e,t,i){return(t=Mi(t))in e?Object.defineProperty(e,t,{value:i,enumerable:!0,configurable:!0,writable:!0}):e[t]=i,e}function Mi(e){var t=Ei(e,"string");return ve(t)=="symbol"?t:t+""}function Ei(e,t){if(ve(e)!="object"||!e)return e;var i=e[Symbol.toPrimitive];if(i!==void 0){var o=i.call(e,t);if(ve(o)!="object")return o;throw new TypeError("@@toPrimitive must return a primitive value.")}return(t==="string"?String:Number)(e)}var ce={name:"Divider",extends:Ti,inheritAttrs:!1,computed:{dataP:function(){return re(De(De(De({},this.align,this.align),this.layout,this.layout),this.type,this.type))}}},$i=["aria-orientation","data-p"],Pi=["data-p"];function xi(e,t,i,o,c,n){return p(),m("div",b({class:e.cx("root"),style:e.sx("root"),role:"separator","aria-orientation":e.layout,"data-p":n.dataP},e.ptmi("root")),[e.$slots.default?(p(),m("div",b({key:0,class:e.cx("content"),"data-p":n.dataP},e.ptm("content")),[T(e.$slots,"default")],16,Pi)):O("",!0)],16,$i)}ce.render=xi;var Vi=`
    .p-timeline {
        display: flex;
        flex-grow: 1;
        flex-direction: column;
        direction: ltr;
    }

    .p-timeline-left .p-timeline-event-opposite {
        text-align: right;
    }

    .p-timeline-left .p-timeline-event-content {
        text-align: left;
    }

    .p-timeline-right .p-timeline-event {
        flex-direction: row-reverse;
    }

    .p-timeline-right .p-timeline-event-opposite {
        text-align: left;
    }

    .p-timeline-right .p-timeline-event-content {
        text-align: right;
    }

    .p-timeline-vertical.p-timeline-alternate .p-timeline-event:nth-child(even) {
        flex-direction: row-reverse;
    }

    .p-timeline-vertical.p-timeline-alternate .p-timeline-event:nth-child(odd) .p-timeline-event-opposite {
        text-align: right;
    }

    .p-timeline-vertical.p-timeline-alternate .p-timeline-event:nth-child(odd) .p-timeline-event-content {
        text-align: left;
    }

    .p-timeline-vertical.p-timeline-alternate .p-timeline-event:nth-child(even) .p-timeline-event-opposite {
        text-align: left;
    }

    .p-timeline-vertical.p-timeline-alternate .p-timeline-event:nth-child(even) .p-timeline-event-content {
        text-align: right;
    }

    .p-timeline-vertical .p-timeline-event-opposite,
    .p-timeline-vertical .p-timeline-event-content {
        padding: dt('timeline.vertical.event.content.padding');
    }

    .p-timeline-vertical .p-timeline-event-connector {
        width: dt('timeline.event.connector.size');
    }

    .p-timeline-event {
        display: flex;
        position: relative;
        min-height: dt('timeline.event.min.height');
    }

    .p-timeline-event:last-child {
        min-height: 0;
    }

    .p-timeline-event-opposite {
        flex: 1;
    }

    .p-timeline-event-content {
        flex: 1;
    }

    .p-timeline-event-separator {
        flex: 0;
        display: flex;
        align-items: center;
        flex-direction: column;
    }

    .p-timeline-event-marker {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        position: relative;
        align-self: baseline;
        border-width: dt('timeline.event.marker.border.width');
        border-style: solid;
        border-color: dt('timeline.event.marker.border.color');
        border-radius: dt('timeline.event.marker.border.radius');
        width: dt('timeline.event.marker.size');
        height: dt('timeline.event.marker.size');
        background: dt('timeline.event.marker.background');
    }

    .p-timeline-event-marker::before {
        content: ' ';
        border-radius: dt('timeline.event.marker.content.border.radius');
        width: dt('timeline.event.marker.content.size');
        height: dt('timeline.event.marker.content.size');
        background: dt('timeline.event.marker.content.background');
    }

    .p-timeline-event-marker::after {
        content: ' ';
        position: absolute;
        width: 100%;
        height: 100%;
        border-radius: dt('timeline.event.marker.border.radius');
        box-shadow: dt('timeline.event.marker.content.inset.shadow');
    }

    .p-timeline-event-connector {
        flex-grow: 1;
        background: dt('timeline.event.connector.color');
    }

    .p-timeline-horizontal {
        flex-direction: row;
    }

    .p-timeline-horizontal .p-timeline-event {
        flex-direction: column;
        flex: 1;
    }

    .p-timeline-horizontal .p-timeline-event:last-child {
        flex: 0;
    }

    .p-timeline-horizontal .p-timeline-event-separator {
        flex-direction: row;
    }

    .p-timeline-horizontal .p-timeline-event-connector {
        width: 100%;
        height: dt('timeline.event.connector.size');
    }

    .p-timeline-horizontal .p-timeline-event-opposite,
    .p-timeline-horizontal .p-timeline-event-content {
        padding: dt('timeline.horizontal.event.content.padding');
    }

    .p-timeline-horizontal.p-timeline-alternate .p-timeline-event:nth-child(even) {
        flex-direction: column-reverse;
    }

    .p-timeline-bottom .p-timeline-event {
        flex-direction: column-reverse;
    }
`,Ni={root:function(t){var i=t.props;return["p-timeline p-component","p-timeline-"+i.align,"p-timeline-"+i.layout]},event:"p-timeline-event",eventOpposite:"p-timeline-event-opposite",eventSeparator:"p-timeline-event-separator",eventMarker:"p-timeline-event-marker",eventConnector:"p-timeline-event-connector",eventContent:"p-timeline-event-content"},Ri=we.extend({name:"timeline",style:Vi,classes:Ni}),Ki={name:"BaseTimeline",extends:Ne,props:{value:null,align:{mode:String,default:"left"},layout:{mode:String,default:"vertical"},dataKey:null},style:Ri,provide:function(){return{$pcTimeline:this,$parentInstance:this}}};function ye(e){"@babel/helpers - typeof";return ye=typeof Symbol=="function"&&typeof Symbol.iterator=="symbol"?function(t){return typeof t}:function(t){return t&&typeof Symbol=="function"&&t.constructor===Symbol&&t!==Symbol.prototype?"symbol":typeof t},ye(e)}function We(e,t,i){return(t=zi(t))in e?Object.defineProperty(e,t,{value:i,enumerable:!0,configurable:!0,writable:!0}):e[t]=i,e}function zi(e){var t=ji(e,"string");return ye(t)=="symbol"?t:t+""}function ji(e,t){if(ye(e)!="object"||!e)return e;var i=e[Symbol.toPrimitive];if(i!==void 0){var o=i.call(e,t);if(ye(o)!="object")return o;throw new TypeError("@@toPrimitive must return a primitive value.")}return(t==="string"?String:Number)(e)}var Ze={name:"Timeline",extends:Ki,inheritAttrs:!1,methods:{getKey:function(t,i){return this.dataKey?oe(t,this.dataKey):i},getPTOptions:function(t,i){return this.ptm(t,{context:{index:i,count:this.value.length}})}},computed:{dataP:function(){return re(We(We({},this.layout,this.layout),this.align,this.align))}}},Gi=["data-p"],Ui=["data-p"],_i=["data-p"],Hi=["data-p"],Bi=["data-p"],Ji=["data-p"],qi=["data-p"];function Qi(e,t,i,o,c,n){return p(),m("div",b({class:e.cx("root")},e.ptmi("root"),{"data-p":n.dataP}),[(p(!0),m(ne,null,ke(e.value,function(I,y){return p(),m("div",b({key:n.getKey(I,y),class:e.cx("event")},{ref_for:!0},n.getPTOptions("event",y),{"data-p":n.dataP}),[s("div",b({class:e.cx("eventOpposite",{index:y})},{ref_for:!0},n.getPTOptions("eventOpposite",y),{"data-p":n.dataP}),[T(e.$slots,"opposite",{item:I,index:y})],16,_i),s("div",b({class:e.cx("eventSeparator")},{ref_for:!0},n.getPTOptions("eventSeparator",y),{"data-p":n.dataP}),[T(e.$slots,"marker",{item:I,index:y},function(){return[s("div",b({class:e.cx("eventMarker")},{ref_for:!0},n.getPTOptions("eventMarker",y),{"data-p":n.dataP}),null,16,Bi)]}),y!==e.value.length-1?T(e.$slots,"connector",{key:0,item:I,index:y},function(){return[s("div",b({class:e.cx("eventConnector")},{ref_for:!0},n.getPTOptions("eventConnector",y),{"data-p":n.dataP}),null,16,Ji)]}):O("",!0)],16,Hi),s("div",b({class:e.cx("eventContent")},{ref_for:!0},n.getPTOptions("eventContent",y),{"data-p":n.dataP}),[T(e.$slots,"content",{item:I,index:y})],16,qi)],16,Ui)}),128))],16,Gi)}Ze.render=Qi;const Wi={class:"analysis-job-detail"},Yi={class:"job-overview"},Xi={class:"job-header"},Zi={class:"job-title"},en={class:"job-actions"},tn={class:"job-metadata"},nn={class:"metadata-grid"},sn={class:"metadata-item"},ln={class:"metadata-value"},on={class:"metadata-item"},rn={class:"metadata-item"},an={key:0,class:"metadata-item"},dn={class:"metadata-item"},cn={key:1,class:"metadata-item"},un={class:"job-progress"},pn={class:"progress-overview"},hn={class:"progress-circle"},fn={class:"circle-container"},mn={class:"progress-ring",width:"120",height:"120"},vn=["stroke","stroke-dasharray","stroke-dashoffset"],yn={class:"progress-text"},bn={class:"progress-percentage"},gn={class:"progress-stats"},On={class:"stat-item"},In={class:"stat-value"},kn={class:"stat-item"},wn={class:"stat-value"},Sn={class:"stat-item"},Cn={class:"stat-value"},An={key:0,class:"stat-item"},Ln={class:"stat-value"},Fn={class:"progress-bars"},Dn={class:"progress-item"},Tn={class:"progress-header"},Mn={class:"progress-value"},En={key:0,class:"progress-item"},$n={class:"progress-header"},Pn={class:"progress-value"},xn={class:"job-status"},Vn={class:"status-display"},Nn={class:"status-icon"},Rn={key:0,class:"pi pi-spin pi-spinner"},Kn={key:1,class:"pi pi-check-circle text-green-500"},zn={key:2,class:"pi pi-times-circle text-red-500"},jn={key:3,class:"pi pi-ban text-orange-500"},Gn={key:4,class:"pi pi-clock text-blue-500"},Un={class:"status-content"},_n={class:"status-message"},Hn={key:0,class:"status-time"},Bn={key:0,class:"job-error"},Jn={class:"error-display"},qn={class:"error-content"},Qn={class:"error-message"},Wn={class:"error-actions"},Yn={key:1,class:"performance-metrics"},Xn={class:"metrics-grid"},Zn={class:"metric-card"},es={class:"metric-content"},ts={class:"metric-value"},is={class:"metric-card"},ns={class:"metric-content"},ss={class:"metric-value"},ls={key:0,class:"metric-card"},os={class:"metric-content"},rs={class:"metric-value"},as={key:1,class:"metric-card"},ds={class:"metric-content"},cs={class:"metric-value"},us={class:"activity-timeline"},ps={class:"timeline-content"},hs={class:"timeline-title"},fs={class:"timeline-description"},ms={class:"timeline-time"},vs=Pe({__name:"AnalysisJobDetail",props:{job:{}},emits:["refresh","cancel","retry","export"],setup(e,{emit:t}){const i=e,o=xe(),c=ue(()=>2*Math.PI*52),n=ue(()=>{const l=k(i.job);return c.value-l/100*c.value}),I=l=>{switch(l){case"COMPLETED":return"success";case"RUNNING":return"info";case"FAILED":return"danger";case"CANCELLED":return"warning";case"PENDING":return"secondary";default:return"secondary"}},y=l=>`job-status-${l.toLowerCase()}`,k=l=>!l.totalArticles||l.totalArticles===0?0:Math.min(100,(l.processedArticles||0)/l.totalArticles*100),V=l=>k(l),_=l=>{switch(l){case"COMPLETED":return"var(--green-500)";case"RUNNING":return"var(--blue-500)";case"FAILED":return"var(--red-500)";case"CANCELLED":return"var(--orange-500)";default:return"var(--surface-border)"}},M=l=>!l.totalArticles||l.totalArticles===0?0:Math.round((l.processedArticles||0)/l.totalArticles*100),K=l=>!l.processedArticles||l.processedArticles===0?"0.00":((l.predictionsFound||0)/l.processedArticles).toFixed(2),B=l=>{if(!l.processedArticles||l.processedArticles===0)return"N/A";const d=G(l);if(!d)return"N/A";const w=d/l.processedArticles;return w<1e3?`${Math.round(w)}ms`:`${(w/1e3).toFixed(1)}s`},j=l=>{if(!l.processedArticles||l.processedArticles===0)return"0";const d=G(l);return d?(l.processedArticles/(d/6e4)).toFixed(1):"0"},E=l=>{if(!l.predictionsFound||l.predictionsFound===0)return"0";const d=G(l);return d?(l.predictionsFound/(d/6e4)).toFixed(1):"0"},v=l=>{if(l.status!=="RUNNING"||!l.processedArticles||l.processedArticles===0)return"N/A";const d=G(l);if(!d)return"N/A";const w=l.processedArticles/(d/1e3),R=(l.totalArticles-l.processedArticles)/w,Q=new Date;return new Date(Q.getTime()+R*1e3).toLocaleTimeString()},G=l=>{if(!l.startedAt)return null;const d=new Date(l.startedAt);return(l.completedAt?new Date(l.completedAt):new Date).getTime()-d.getTime()},J=l=>{switch(l.status){case"PENDING":return"Job is queued and waiting to start";case"RUNNING":return`Processing articles... ${l.processedArticles||0} of ${l.totalArticles||0} completed`;case"COMPLETED":return`Job completed successfully. Found ${l.predictionsFound||0} predictions.`;case"FAILED":return"Job failed during processing";case"CANCELLED":return"Job was cancelled by user";default:return"Unknown status"}},W=l=>{switch(l.status){case"RUNNING":return`Started ${z(l.startedAt)}`;case"COMPLETED":case"FAILED":case"CANCELLED":return l.completedAt?`Finished ${z(l.completedAt)}`:null;default:return l.startedAt?`Created ${z(l.startedAt)}`:null}},$=l=>{const d=[];if(d.push({type:"info",icon:"pi pi-plus",title:"Job Created",description:`Analysis job created for ${l.totalArticles} articles`,time:A(l.startedAt)}),l.status!=="PENDING"&&d.push({type:"info",icon:"pi pi-play",title:"Processing Started",description:`Began processing articles using ${l.analysisType} analysis`,time:A(l.startedAt)}),l.processedArticles>0){const w=[25,50,75],L=k(l);w.forEach(R=>{L>=R&&d.push({type:"success",icon:"pi pi-check",title:`${R}% Complete`,description:`Processed ${Math.round(R/100*l.totalArticles)} articles`,time:"Estimated time"})})}if(l.completedAt){const w={time:A(l.completedAt)};switch(l.status){case"COMPLETED":d.push({...w,type:"success",icon:"pi pi-check-circle",title:"Job Completed",description:`Successfully processed all articles. Found ${l.predictionsFound} predictions.`});break;case"FAILED":d.push({...w,type:"error",icon:"pi pi-times-circle",title:"Job Failed",description:l.errorMessage||"Job failed due to an error"});break;case"CANCELLED":d.push({...w,type:"warning",icon:"pi pi-ban",title:"Job Cancelled",description:"Job was cancelled by user request"});break}}return d.reverse()},pe=l=>l.status==="RUNNING"||l.status==="PENDING",Y=l=>l.status==="FAILED"||l.status==="CANCELLED",F=l=>l.status==="COMPLETED"&&l.predictionsFound>0,A=l=>new Date(l).toLocaleString(),z=l=>{const d=new Date(l),L=new Date().getTime()-d.getTime(),R=Math.floor(L/1e3),Q=Math.floor(R/60),ae=Math.floor(Q/60);return R<60?`${R} seconds ago`:Q<60?`${Q} minutes ago`:ae<24?`${ae} hours ago`:d.toLocaleString()},ee=l=>{const d=G(l);if(!d)return"-";const w=Math.floor(d/1e3),L=Math.floor(w/60),R=Math.floor(L/60),Q=Math.floor(R/24);return Q>0?`${Q}d ${R%24}h ${L%60}m`:R>0?`${R}h ${L%60}m ${w%60}s`:L>0?`${L}m ${w%60}s`:`${w}s`},Se=l=>ee(l),Ce=async l=>{try{await navigator.clipboard.writeText(l),o.add({severity:"success",summary:"Copied",detail:"Job ID copied to clipboard",life:2e3})}catch{o.add({severity:"error",summary:"Copy Failed",detail:"Failed to copy job ID",life:3e3})}},Ae=async()=>{try{await navigator.clipboard.writeText(i.job.errorMessage),o.add({severity:"success",summary:"Copied",detail:"Error message copied to clipboard",life:2e3})}catch{o.add({severity:"error",summary:"Copy Failed",detail:"Failed to copy error message",life:3e3})}};return(l,d)=>{const w=$e("tooltip");return p(),m("div",Wi,[s("div",Yi,[s("div",Xi,[s("div",Zi,[s("h4",null,g(l.job.jobId),1),h(f(he),{value:l.job.status,severity:I(l.job.status),class:U(y(l.job.status))},null,8,["value","severity","class"])]),s("div",en,[q(h(f(x),{icon:"pi pi-refresh",severity:"secondary",size:"small",onClick:d[0]||(d[0]=L=>l.$emit("refresh"))},null,512),[[w,"Refresh",void 0,{top:!0}]]),pe(l.job)?q((p(),N(f(x),{key:0,icon:"pi pi-times",severity:"danger",size:"small",onClick:d[1]||(d[1]=L=>l.$emit("cancel",l.job.jobId))},null,512)),[[w,"Cancel job",void 0,{top:!0}]]):O("",!0),Y(l.job)?q((p(),N(f(x),{key:1,icon:"pi pi-refresh",severity:"warning",size:"small",onClick:d[2]||(d[2]=L=>l.$emit("retry",l.job.jobId))},null,512)),[[w,"Retry job",void 0,{top:!0}]]):O("",!0),F(l.job)?q((p(),N(f(x),{key:2,icon:"pi pi-download",severity:"info",size:"small",onClick:d[3]||(d[3]=L=>l.$emit("export",l.job.jobId))},null,512)),[[w,"Export results",void 0,{top:!0}]]):O("",!0)])]),s("div",tn,[s("div",nn,[s("div",sn,[d[5]||(d[5]=s("label",null,"Job ID",-1)),s("div",ln,[s("code",null,g(l.job.jobId),1),q(h(f(x),{icon:"pi pi-copy",class:"p-button-text p-button-sm",onClick:d[4]||(d[4]=L=>Ce(l.job.jobId))},null,512),[[w,"Copy Job ID"]])])]),s("div",on,[d[6]||(d[6]=s("label",null,"Analysis Type",-1)),s("span",null,[h(f(he),{value:l.job.analysisType,severity:l.job.analysisType==="llm"?"info":"secondary"},null,8,["value","severity"])])]),s("div",rn,[d[7]||(d[7]=s("label",null,"Started",-1)),s("span",null,g(A(l.job.startedAt)),1)]),l.job.completedAt?(p(),m("div",an,[d[8]||(d[8]=s("label",null,"Completed",-1)),s("span",null,g(A(l.job.completedAt)),1)])):O("",!0),s("div",dn,[d[9]||(d[9]=s("label",null,"Duration",-1)),s("span",null,g(ee(l.job)),1)]),l.job.status==="RUNNING"?(p(),m("div",cn,[d[10]||(d[10]=s("label",null,"Elapsed Time",-1)),s("span",null,g(Se(l.job)),1)])):O("",!0)])])]),h(f(ce)),s("div",un,[d[19]||(d[19]=s("h5",null,"Progress & Statistics",-1)),s("div",pn,[s("div",hn,[s("div",fn,[(p(),m("svg",mn,[d[11]||(d[11]=s("circle",{class:"progress-ring-background",stroke:"var(--surface-border)","stroke-width":"8",fill:"transparent",r:"52",cx:"60",cy:"60"},null,-1)),s("circle",{class:"progress-ring-progress",stroke:_(l.job.status),"stroke-width":"8",fill:"transparent",r:"52",cx:"60",cy:"60","stroke-dasharray":c.value,"stroke-dashoffset":n.value,"stroke-linecap":"round"},null,8,vn)])),s("div",yn,[s("span",bn,g(Math.round(k(l.job)))+"%",1),d[12]||(d[12]=s("span",{class:"progress-label"},"Complete",-1))])])]),s("div",gn,[s("div",On,[s("div",In,g(l.job.totalArticles||0),1),d[13]||(d[13]=s("div",{class:"stat-label"},"Total Articles",-1))]),s("div",kn,[s("div",wn,g(l.job.processedArticles||0),1),d[14]||(d[14]=s("div",{class:"stat-label"},"Processed",-1))]),s("div",Sn,[s("div",Cn,g(l.job.predictionsFound||0),1),d[15]||(d[15]=s("div",{class:"stat-label"},"Predictions Found",-1))]),l.job.status==="COMPLETED"?(p(),m("div",An,[s("div",Ln,g(M(l.job))+"%",1),d[16]||(d[16]=s("div",{class:"stat-label"},"Success Rate",-1))])):O("",!0)])]),s("div",Fn,[s("div",Dn,[s("div",Tn,[d[17]||(d[17]=s("span",{class:"progress-label"},"Articles Processed",-1)),s("span",Mn,g(l.job.processedArticles||0)+" / "+g(l.job.totalArticles||0),1)]),h(f(Te),{value:V(l.job),showValue:!1,class:"progress-bar success"},null,8,["value"])]),l.job.predictionsFound>0?(p(),m("div",En,[s("div",$n,[d[18]||(d[18]=s("span",{class:"progress-label"},"Prediction Density",-1)),s("span",Pn,g(K(l.job))+" per article",1)]),h(f(Te),{value:Math.min(100,parseFloat(K(l.job))*20),showValue:!1,class:"progress-bar info"},null,8,["value"])])):O("",!0)])]),h(f(ce)),s("div",xn,[d[20]||(d[20]=s("h5",null,"Current Status",-1)),s("div",Vn,[s("div",Nn,[l.job.status==="RUNNING"?(p(),m("i",Rn)):l.job.status==="COMPLETED"?(p(),m("i",Kn)):l.job.status==="FAILED"?(p(),m("i",zn)):l.job.status==="CANCELLED"?(p(),m("i",jn)):(p(),m("i",Gn))]),s("div",Un,[s("div",_n,g(J(l.job)),1),W(l.job)?(p(),m("div",Hn,g(W(l.job)),1)):O("",!0)])])]),l.job.errorMessage?(p(),m("div",Bn,[h(f(ce)),d[21]||(d[21]=s("h5",null,"Error Details",-1)),s("div",Jn,[h(f(Wt),{severity:"error",closable:!1},{default:C(()=>[s("div",qn,[s("div",Qn,g(l.job.errorMessage),1),s("div",Wn,[h(f(x),{label:"Copy Error",icon:"pi pi-copy",severity:"secondary",size:"small",onClick:Ae})])])]),_:1})])])):O("",!0),l.job.status==="COMPLETED"||l.job.status==="RUNNING"?(p(),m("div",Yn,[h(f(ce)),d[30]||(d[30]=s("h5",null,"Performance Metrics",-1)),s("div",Xn,[s("div",Zn,[d[23]||(d[23]=s("div",{class:"metric-icon"},[s("i",{class:"pi pi-clock"})],-1)),s("div",es,[s("div",ts,g(B(l.job)),1),d[22]||(d[22]=s("div",{class:"metric-label"},"Avg. Processing Time",-1))])]),s("div",is,[d[25]||(d[25]=s("div",{class:"metric-icon"},[s("i",{class:"pi pi-chart-line"})],-1)),s("div",ns,[s("div",ss,g(j(l.job)),1),d[24]||(d[24]=s("div",{class:"metric-label"},"Articles/Min",-1))])]),l.job.predictionsFound>0?(p(),m("div",ls,[d[27]||(d[27]=s("div",{class:"metric-icon"},[s("i",{class:"pi pi-search"})],-1)),s("div",os,[s("div",rs,g(E(l.job)),1),d[26]||(d[26]=s("div",{class:"metric-label"},"Predictions/Min",-1))])])):O("",!0),l.job.status==="RUNNING"?(p(),m("div",as,[d[29]||(d[29]=s("div",{class:"metric-icon"},[s("i",{class:"pi pi-forward"})],-1)),s("div",ds,[s("div",cs,g(v(l.job)),1),d[28]||(d[28]=s("div",{class:"metric-label"},"Est. Completion",-1))])])):O("",!0)])])):O("",!0),s("div",us,[h(f(ce)),d[31]||(d[31]=s("h5",null,"Activity Timeline",-1)),h(f(Ze),{value:$(l.job),class:"job-timeline"},{marker:C(({item:L})=>[s("div",{class:U(["timeline-marker",L.type])},[s("i",{class:U(L.icon)},null,2)],2)]),content:C(({item:L})=>[s("div",ps,[s("div",hs,g(L.title),1),s("div",fs,g(L.description),1),s("div",ms,g(L.time),1)])]),_:1},8,["value"])])])}}}),ys=Ve(vs,[["__scopeId","data-v-67dd9fa4"]]),bs={class:"analysis-history"},gs={class:"history-header"},Os={class:"header-actions"},Is={class:"quick-filters"},ks={class:"filter-chips"},ws={key:0,class:"filter-summary"},Ss={class:"filter-count"},Cs={class:"job-id-cell"},As={class:"job-id"},Ls={class:"datetime-cell"},Fs={class:"date"},Ds={class:"time"},Ts={key:0,class:"datetime-cell"},Ms={class:"date"},Es={class:"time"},$s={key:1,class:"text-muted"},Ps={class:"duration"},xs={class:"article-progress"},Vs={class:"progress-text"},Ns={class:"predictions-cell"},Rs={key:1,class:"text-muted"},Ks={class:"action-buttons"},zs={class:"filter-form"},js={class:"field"},Gs={class:"field"},Us={class:"field"},_s={class:"field"},Hs={class:"field"},Bs={class:"field"},Js={class:"cancel-confirmation"},qs={class:"confirmation-content"},Qs={class:"job-info"},Ws=Pe({__name:"AnalysisHistory",props:{history:{default:()=>[]},autoRefresh:{type:Boolean,default:!0},refreshInterval:{default:3e4},pageSize:{default:20}},emits:["view-results","refresh"],setup(e,{emit:t}){const i=e,o=t,c=xe(),n=H(!1),I=H(!1),y=H([]),k=H(!1),V=H(!1),_=H(!1),M=H(null),K=H(null),B=H(0),j=H(null),E=H({status:[],analysisType:[],dateFrom:null,dateTo:null,minPredictions:null,searchTerm:""}),v=H({status:[],analysisType:[],dateFrom:null,dateTo:null,minPredictions:null,searchTerm:""}),G=[{label:"Pending",value:"PENDING"},{label:"Running",value:"RUNNING"},{label:"Completed",value:"COMPLETED"},{label:"Failed",value:"FAILED"},{label:"Cancelled",value:"CANCELLED"}],J=[{label:"Mock",value:"mock"},{label:"LLM",value:"llm"}],W=[{label:"Running",value:"RUNNING"},{label:"Completed",value:"COMPLETED"},{label:"Failed",value:"FAILED"}],$=ue(()=>{let r=[...y.value];if(j.value&&(r=r.filter(a=>a.status===j.value)),v.value.status.length>0&&(r=r.filter(a=>v.value.status.includes(a.status))),v.value.analysisType.length>0&&(r=r.filter(a=>v.value.analysisType.includes(a.analysisType))),v.value.dateFrom){const a=new Date(v.value.dateFrom);r=r.filter(S=>new Date(S.startedAt)>=a)}if(v.value.dateTo){const a=new Date(v.value.dateTo);a.setHours(23,59,59,999),r=r.filter(S=>new Date(S.startedAt)<=a)}if(v.value.minPredictions!==null&&v.value.minPredictions>=0&&(r=r.filter(a=>(a.predictionsFound||0)>=(v.value.minPredictions||0))),v.value.searchTerm){const a=v.value.searchTerm.toLowerCase();r=r.filter(S=>S.jobId.toLowerCase().includes(a)||S.errorMessage&&S.errorMessage.toLowerCase().includes(a))}return r}),pe=ue(()=>{const r=B.value,a=r+i.pageSize;return $.value.slice(r,a)}),Y=ue(()=>j.value!==null||v.value.status.length>0||v.value.analysisType.length>0||v.value.dateFrom!==null||v.value.dateTo!==null||v.value.minPredictions!==null||v.value.searchTerm!==""),F=ue(()=>n.value?"Loading analysis jobs...":Y.value?"No jobs match the current filters":"No analysis jobs found"),A=async()=>{n.value=!0;try{y.value=mt()}catch{c.add({severity:"error",summary:"Load Failed",detail:"Failed to load analysis history",life:5e3})}finally{n.value=!1}},z=async()=>{I.value=!0;try{await A(),o("refresh"),c.add({severity:"success",summary:"Success",detail:"Analysis history refreshed",life:3e3})}catch{c.add({severity:"error",summary:"Error",detail:"Failed to refresh history",life:5e3})}finally{I.value=!1}},ee=r=>y.value.filter(a=>a.status===r).length,Se=r=>{j.value===r?j.value=null:j.value=r,B.value=0},Ce=()=>{j.value=null},Ae=()=>{j.value=null,v.value={status:[],analysisType:[],dateFrom:null,dateTo:null,minPredictions:null,searchTerm:""},E.value={...v.value},B.value=0},l=()=>{v.value={...E.value},V.value=!1,B.value=0},d=()=>{E.value={status:[],analysisType:[],dateFrom:null,dateTo:null,minPredictions:null,searchTerm:""}},w=async r=>{M.value=r,k.value=!0},L=async()=>{if(M.value){const r=y.value.find(a=>a.jobId===M.value.jobId);r&&(M.value={...r})}},R=r=>{K.value=r,_.value=!0},Q=async()=>{if(K.value)try{await new Promise(a=>setTimeout(a,500));const r=y.value.findIndex(a=>a.jobId===K.value.jobId);r!==-1&&(y.value[r].status="CANCELLED",y.value[r].completedAt=new Date().toISOString()),c.add({severity:"success",summary:"Job Cancelled",detail:`Analysis job ${K.value.jobId} has been cancelled`,life:3e3})}catch{c.add({severity:"error",summary:"Cancel Failed",detail:"Failed to cancel analysis job",life:5e3})}finally{_.value=!1,K.value=null}},ae=async r=>{try{Ge(r),await new Promise(a=>setTimeout(a,1e3)),c.add({severity:"success",summary:"Analysis Retried",detail:"Analysis job has been queued for retry",life:3e3}),await z()}catch{c.add({severity:"error",summary:"Retry Failed",detail:"Failed to retry analysis",life:5e3})}},Ke=async r=>{try{const a=ft(r),S=pt(a);ht(S,`analysis-${r.jobId}.csv`,"text/csv"),c.add({severity:"success",summary:"Export Complete",detail:"Results exported successfully",life:3e3})}catch{c.add({severity:"error",summary:"Export Failed",detail:"Failed to export results",life:5e3})}},et=async r=>{const a=y.value.find(S=>S.jobId===r);a&&(K.value=a,k.value=!1,_.value=!0)},tt=async r=>{const a=y.value.find(S=>S.jobId===r);a&&(k.value=!1,await ae(a))},it=async r=>{const a=y.value.find(S=>S.jobId===r);a&&await Ke(a)},nt=r=>{switch(r){case"COMPLETED":return"success";case"RUNNING":return"info";case"FAILED":return"danger";case"CANCELLED":return"warning";case"PENDING":return"secondary";default:return"secondary"}},st=r=>`job-status-${r.toLowerCase()}`,lt=r=>!r.totalArticles||r.totalArticles===0?0:Math.min(100,(r.processedArticles||0)/r.totalArticles*100),ot=r=>r.status==="COMPLETED"&&r.predictionsFound>0,rt=r=>r.status==="FAILED"||r.status==="CANCELLED",at=r=>r.status==="RUNNING"||r.status==="PENDING",dt=r=>r.length>8?r.substring(0,8)+"...":r,ze=r=>new Date(r).toLocaleDateString(),je=r=>new Date(r).toLocaleTimeString(),ct=r=>{if(!r.startedAt)return"-";const a=new Date(r.startedAt),D=(r.completedAt?new Date(r.completedAt):new Date).getTime()-a.getTime(),u=Math.floor(D/1e3),P=Math.floor(u/60),ge=Math.floor(P/60);return ge>0?`${ge}h ${P%60}m`:P>0?`${P}m ${u%60}s`:`${u}s`},ut=async r=>{try{await navigator.clipboard.writeText(r),c.add({severity:"success",summary:"Copied",detail:"Job ID copied to clipboard",life:2e3})}catch{c.add({severity:"error",summary:"Copy Failed",detail:"Failed to copy job ID",life:3e3})}},pt=r=>{const a=["Prediction","Rating","Confidence","Article Title","Author","Context"],S=r.map(D=>{var u;return[`"${D.predictionText}"`,D.rating,D.confidenceScore,`"${D.article.title}"`,`"${((u=D.article.author)==null?void 0:u.name)||"Unknown"}"`,`"${D.context}"`]});return[a.join(","),...S.map(D=>D.join(","))].join(`
`)},ht=(r,a,S)=>{const D=new Blob([r],{type:S}),u=URL.createObjectURL(D),P=document.createElement("a");P.href=u,P.download=a,document.body.appendChild(P),P.click(),document.body.removeChild(P),URL.revokeObjectURL(u)},ft=r=>{Ge(r);const a=["The market will experience significant volatility in the coming months","Technology stocks are expected to outperform traditional sectors","Interest rates will likely remain stable through the next quarter"];return Array.from({length:r.predictionsFound},(S,D)=>({id:`pred-${r.jobId}-${D}`,predictionText:a[Math.floor(Math.random()*a.length)],rating:Math.floor(Math.random()*5)+1,confidenceScore:Math.random(),context:`Sample context for prediction ${D+1}...`,article:{id:D+1,title:`Sample Article ${D+1}`,author:{name:"Sample Author"}}}))},mt=()=>{const r=["COMPLETED","RUNNING","FAILED","CANCELLED","PENDING"],a=["mock","llm"];return Array.from({length:50},(S,D)=>{const u=r[Math.floor(Math.random()*r.length)],P=new Date(Date.now()-Math.random()*30*24*60*60*1e3).toISOString(),ge=u==="COMPLETED"||u==="FAILED"||u==="CANCELLED"?new Date(new Date(P).getTime()+Math.random()*60*60*1e3).toISOString():null;return{id:D+1,jobId:`analysis-${Date.now()}-${D}`,status:u,startedAt:P,completedAt:ge,totalArticles:Math.floor(Math.random()*100)+10,processedArticles:u==="RUNNING"?Math.floor(Math.random()*50):u==="COMPLETED"?Math.floor(Math.random()*100)+10:0,predictionsFound:u==="COMPLETED"?Math.floor(Math.random()*50):0,analysisType:a[Math.floor(Math.random()*a.length)],errorMessage:u==="FAILED"?"Sample error message for testing":null}})},Ge=(...r)=>{};_e(()=>i.history,r=>{r&&r.length>0&&(y.value=r)},{immediate:!0});let be=null;const Ue=()=>{i.autoRefresh&&i.refreshInterval>0&&(be=setInterval(()=>{!n.value&&!k.value&&z()},i.refreshInterval))},vt=()=>{be&&(clearInterval(be),be=null)};return Ye(()=>{(!i.history||i.history.length===0)&&A(),Ue()}),_e(()=>i.autoRefresh,r=>{r?Ue():vt()}),(r,a)=>{var D;const S=$e("tooltip");return p(),m("div",bs,[s("div",gs,[a[12]||(a[12]=s("div",{class:"header-content"},[s("h3",null,"Analysis History"),s("p",{class:"header-description"},"View and manage prediction analysis jobs")],-1)),s("div",Os,[h(f(x),{icon:"pi pi-refresh",label:"Refresh",onClick:z,loading:I.value,severity:"secondary",size:"small"},null,8,["loading"]),h(f(x),{icon:"pi pi-filter",label:"Filter",onClick:a[0]||(a[0]=u=>V.value=!0),severity:"secondary",size:"small"})])]),s("div",Is,[s("div",ks,[(p(),m(ne,null,ke(W,u=>h(f(Re),{key:u.value,label:`${u.label} (${ee(u.value)})`,class:U({active:j.value===u.value}),onClick:P=>Se(u.value),removable:j.value===u.value,onRemove:Ce},null,8,["label","class","onClick","removable"])),64))]),Y.value?(p(),m("div",ws,[s("span",Ss,g($.value.length)+" of "+g(y.value.length)+" jobs",1),h(f(x),{icon:"pi pi-times",label:"Clear All",onClick:Ae,severity:"secondary",size:"small",text:""})])):O("",!0)]),h(f(qt),{value:pe.value,loading:n.value,paginator:!0,rows:r.pageSize,totalRecords:$.value.length,lazy:!1,first:B.value,"onUpdate:first":a[1]||(a[1]=u=>B.value=u),sortField:"startedAt",sortOrder:-1,class:"analysis-history-table",responsiveLayout:"scroll",emptyMessage:F.value,stripedRows:""},{default:C(()=>[h(f(Z),{field:"jobId",header:"Job ID",sortable:""},{body:C(({data:u})=>[s("div",Cs,[s("code",As,g(dt(u.jobId)),1),q(h(f(x),{icon:"pi pi-copy",class:"p-button-text p-button-sm copy-btn",onClick:P=>ut(u.jobId)},null,8,["onClick"]),[[S,"Copy full Job ID"]])])]),_:1}),h(f(Z),{field:"status",header:"Status",sortable:""},{body:C(({data:u})=>[h(f(he),{value:u.status,severity:nt(u.status),class:U(st(u.status))},null,8,["value","severity","class"])]),_:1}),h(f(Z),{field:"startedAt",header:"Started",sortable:""},{body:C(({data:u})=>[s("div",Ls,[s("span",Fs,g(ze(u.startedAt)),1),s("span",Ds,g(je(u.startedAt)),1)])]),_:1}),h(f(Z),{field:"completedAt",header:"Completed",sortable:""},{body:C(({data:u})=>[u.completedAt?(p(),m("div",Ts,[s("span",Ms,g(ze(u.completedAt)),1),s("span",Es,g(je(u.completedAt)),1)])):(p(),m("span",$s,"-"))]),_:1}),h(f(Z),{field:"duration",header:"Duration",sortable:""},{body:C(({data:u})=>[s("span",Ps,g(ct(u)),1)]),_:1}),h(f(Z),{field:"totalArticles",header:"Articles",sortable:""},{body:C(({data:u})=>[s("div",xs,[s("span",Vs,g(u.processedArticles||0)+" / "+g(u.totalArticles),1),u.status==="RUNNING"?(p(),N(f(Te),{key:0,value:lt(u),showValue:!1,class:"mini-progress"},null,8,["value"])):O("",!0)])]),_:1}),h(f(Z),{field:"predictionsFound",header:"Predictions",sortable:""},{body:C(({data:u})=>[s("div",Ns,[u.predictionsFound>0?(p(),N(f(he),{key:0,value:u.predictionsFound,severity:"success"},null,8,["value"])):(p(),m("span",Rs,"0"))])]),_:1}),h(f(Z),{field:"analysisType",header:"Type",sortable:""},{body:C(({data:u})=>[h(f(he),{value:u.analysisType,severity:u.analysisType==="llm"?"info":"secondary"},null,8,["value","severity"])]),_:1}),h(f(Z),{header:"Actions",exportable:!1},{body:C(({data:u})=>[s("div",Ks,[q(h(f(x),{icon:"pi pi-eye",class:"p-button-text p-button-sm",onClick:P=>w(u),severity:"info"},null,8,["onClick"]),[[S,"View Details"]]),q(h(f(x),{icon:"pi pi-download",class:"p-button-text p-button-sm",onClick:P=>Ke(u),disabled:!ot(u),severity:"secondary"},null,8,["onClick","disabled"]),[[S,"Export Results"]]),q(h(f(x),{icon:"pi pi-refresh",class:"p-button-text p-button-sm",onClick:P=>ae(u),disabled:!rt(u),severity:"warning"},null,8,["onClick","disabled"]),[[S,"Retry Analysis"]]),q(h(f(x),{icon:"pi pi-times",class:"p-button-text p-button-sm",onClick:P=>R(u),disabled:!at(u),severity:"danger"},null,8,["onClick","disabled"]),[[S,"Cancel Analysis"]])])]),_:1})]),_:1},8,["value","loading","rows","totalRecords","first","emptyMessage"]),h(f(Fe),{visible:k.value,"onUpdate:visible":a[2]||(a[2]=u=>k.value=u),header:`Analysis Job Details - ${(D=M.value)==null?void 0:D.jobId}`,modal:!0,style:{width:"90vw",maxWidth:"1200px"},maximizable:!0,closable:!0},{default:C(()=>[M.value?(p(),N(ys,{key:0,job:M.value,onRefresh:L,onCancel:et,onRetry:tt,onExport:it},null,8,["job"])):O("",!0)]),_:1},8,["visible","header"]),h(f(Fe),{visible:V.value,"onUpdate:visible":a[9]||(a[9]=u=>V.value=u),header:"Filter Analysis Jobs",modal:"",style:{width:"30rem"},breakpoints:{"1199px":"75vw","575px":"90vw"}},{footer:C(()=>[h(f(x),{label:"Clear",severity:"secondary",onClick:d}),h(f(x),{label:"Apply",onClick:l})]),default:C(()=>[s("div",zs,[s("div",js,[a[13]||(a[13]=s("label",{for:"status-filter"},"Status",-1)),h(f(Ee),{id:"status-filter",modelValue:E.value.status,"onUpdate:modelValue":a[3]||(a[3]=u=>E.value.status=u),options:G,optionLabel:"label",optionValue:"value",placeholder:"Select statuses",class:"w-full"},null,8,["modelValue"])]),s("div",Gs,[a[14]||(a[14]=s("label",{for:"analysis-type-filter"},"Analysis Type",-1)),h(f(Ee),{id:"analysis-type-filter",modelValue:E.value.analysisType,"onUpdate:modelValue":a[4]||(a[4]=u=>E.value.analysisType=u),options:J,optionLabel:"label",optionValue:"value",placeholder:"Select types",class:"w-full"},null,8,["modelValue"])]),s("div",Us,[a[15]||(a[15]=s("label",{for:"date-from"},"Date From",-1)),h(f(He),{id:"date-from",modelValue:E.value.dateFrom,"onUpdate:modelValue":a[5]||(a[5]=u=>E.value.dateFrom=u),showIcon:"",dateFormat:"yy-mm-dd",class:"w-full"},null,8,["modelValue"])]),s("div",_s,[a[16]||(a[16]=s("label",{for:"date-to"},"Date To",-1)),h(f(He),{id:"date-to",modelValue:E.value.dateTo,"onUpdate:modelValue":a[6]||(a[6]=u=>E.value.dateTo=u),showIcon:"",dateFormat:"yy-mm-dd",class:"w-full"},null,8,["modelValue"])]),s("div",Hs,[a[17]||(a[17]=s("label",{for:"min-predictions"},"Min Predictions",-1)),h(f(Qt),{id:"min-predictions",modelValue:E.value.minPredictions,"onUpdate:modelValue":a[7]||(a[7]=u=>E.value.minPredictions=u),placeholder:"0",min:0,class:"w-full"},null,8,["modelValue"])]),s("div",Bs,[a[18]||(a[18]=s("label",{for:"search-term"},"Search",-1)),h(f(Xe),{id:"search-term",modelValue:E.value.searchTerm,"onUpdate:modelValue":a[8]||(a[8]=u=>E.value.searchTerm=u),placeholder:"Search job ID or error message",class:"w-full"},null,8,["modelValue"])])])]),_:1},8,["visible"]),h(f(Fe),{visible:_.value,"onUpdate:visible":a[11]||(a[11]=u=>_.value=u),header:"Cancel Analysis Job",modal:"",style:{width:"25rem"}},{footer:C(()=>[h(f(x),{label:"No",severity:"secondary",onClick:a[10]||(a[10]=u=>_.value=!1)}),h(f(x),{label:"Yes, Cancel Job",severity:"danger",onClick:Q})]),default:C(()=>{var u;return[s("div",Js,[a[22]||(a[22]=s("i",{class:"pi pi-exclamation-triangle warning-icon"},null,-1)),s("div",qs,[a[20]||(a[20]=s("p",null,"Are you sure you want to cancel this analysis job?",-1)),s("p",Qs,[a[19]||(a[19]=de("Job ID: ",-1)),s("code",null,g((u=K.value)==null?void 0:u.jobId),1)]),a[21]||(a[21]=s("p",{class:"warning-text"},"This action cannot be undone.",-1))])])]}),_:1},8,["visible"])])}}}),Ys=Ve(Ws,[["__scopeId","data-v-16a78762"]]),Xs={class:"predictions-history"},Zs={class:"page-header"},el={class:"header-actions"},tl=Pe({__name:"PredictionsHistory",setup(e){const t=Ot(),i=xe(),o=H([]),c=()=>{t.push("/predictions/analysis")},n=k=>{console.log("Viewing results for job:",k.jobId)},I=async()=>{try{await y(),i.add({severity:"success",summary:"Success",detail:"Analysis history refreshed successfully",life:3e3})}catch{i.add({severity:"error",summary:"Error",detail:"Failed to refresh analysis history",life:5e3})}},y=async()=>{o.value=Array.from({length:25},(k,V)=>{const _=["COMPLETED","RUNNING","FAILED","CANCELLED","PENDING"],M=_[Math.floor(Math.random()*_.length)],K=new Date(Date.now()-Math.random()*30*24*60*60*1e3).toISOString(),B=M==="COMPLETED"||M==="FAILED"||M==="CANCELLED"?new Date(new Date(K).getTime()+Math.random()*60*60*1e3).toISOString():null;return{id:V+1,jobId:`job-${Date.now()}-${V}`,status:M,startedAt:K,completedAt:B,totalArticles:Math.floor(Math.random()*100)+10,processedArticles:M==="RUNNING"?Math.floor(Math.random()*50):M==="COMPLETED"?Math.floor(Math.random()*100)+10:0,predictionsFound:M==="COMPLETED"?Math.floor(Math.random()*50):0,analysisType:Math.random()>.5?"mock":"llm"}})};return Ye(async()=>{await y()}),(k,V)=>(p(),m("div",Xs,[s("div",Zs,[V[0]||(V[0]=s("div",{class:"header-content"},[s("h1",{class:"page-title"},"Predictions History"),s("p",{class:"page-description"},"View prediction analysis history and results")],-1)),s("div",el,[h(f(x),{icon:"pi pi-search",label:"New Analysis",onClick:c,severity:"primary"})])]),h(Ys,{history:o.value,"auto-refresh":!0,"refresh-interval":3e4,"page-size":20,onViewResults:n,onRefresh:I},null,8,["history"])]))}}),cl=Ve(tl,[["__scopeId","data-v-14ca9b09"]]);export{cl as default};
