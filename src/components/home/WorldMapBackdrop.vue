<template>
  <div class="world-backdrop" aria-hidden="true">
    <div class="grid"></div>
    <div class="glow glow-west"></div>
    <div class="glow glow-east"></div>

    <svg class="world-map" viewBox="0 0 1200 600" fill="none" xmlns="http://www.w3.org/2000/svg">
      <g class="latitude-lines">
        <path d="M60 160H1140" />
        <path d="M40 240H1160" />
        <path d="M30 320H1170" />
        <path d="M45 400H1155" />
      </g>
      <g class="continents">
        <path
          d="M112 182C155 145 207 130 257 143C297 153 327 177 337 210C350 251 330 288 297 318C258 354 211 363 181 337C149 309 118 261 103 228C94 208 96 194 112 182Z"
        />
        <path
          d="M286 336C314 321 349 326 367 349C383 370 379 398 366 426C353 455 337 490 312 513C293 530 268 527 253 509C238 490 241 465 250 438C258 413 261 391 271 367C275 355 278 343 286 336Z"
        />
        <path
          d="M486 146C552 119 622 117 691 136C726 146 762 164 786 188C804 206 819 230 806 250C791 274 753 282 719 284C683 286 656 295 630 315C593 343 549 349 505 337C474 329 446 310 429 282C416 260 415 230 429 208C443 185 461 156 486 146Z"
        />
        <path
          d="M590 354C618 340 652 343 673 364C692 382 697 410 689 440C681 467 666 496 642 517C621 536 592 536 571 519C548 500 543 470 549 441C554 415 564 388 579 369C583 364 586 357 590 354Z"
        />
        <path
          d="M820 172C853 147 903 142 945 149C1004 158 1065 184 1094 224C1112 248 1110 274 1086 292C1058 312 1014 314 976 309C935 303 904 292 875 310C852 324 824 321 803 306C780 288 770 258 778 229C784 207 798 189 820 172Z"
        />
        <path
          d="M942 362C968 348 1000 347 1028 356C1056 365 1081 383 1090 409C1098 432 1088 458 1067 476C1044 496 1012 507 979 506C949 504 921 492 904 468C887 443 886 413 897 393C906 379 922 372 942 362Z"
        />
      </g>
      <g class="hotspots">
        <circle cx="295" cy="212" r="52" />
        <circle cx="610" cy="206" r="74" />
        <circle cx="890" cy="245" r="70" />
        <circle cx="1010" cy="415" r="48" />
      </g>
    </svg>
  </div>
</template>

<style scoped>
.world-backdrop {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

.grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(92, 126, 202, 0.07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(92, 126, 202, 0.07) 1px, transparent 1px);
  background-size: 72px 72px;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.7), transparent 90%);
}

.world-map {
  position: absolute;
  inset: 50% auto auto 50%;
  width: min(1280px, 130vw);
  transform: translate(-50%, -46%);
  opacity: 0.62;
}

.latitude-lines path {
  stroke: rgba(132, 161, 230, 0.18);
  stroke-width: 1.25;
}

.continents path {
  fill: rgba(107, 131, 193, 0.12);
  stroke: rgba(150, 176, 236, 0.34);
  stroke-width: 1.4;
}

.hotspots circle {
  fill: rgba(255, 138, 74, 0.18);
  filter: blur(18px);
}

.glow {
  position: absolute;
  border-radius: 999px;
  filter: blur(70px);
}

.glow-west {
  top: 20%;
  left: 17%;
  width: 220px;
  height: 120px;
  background: rgba(255, 132, 87, 0.22);
}

.glow-east {
  top: 24%;
  right: 16%;
  width: 300px;
  height: 130px;
  background: rgba(255, 188, 107, 0.16);
}
</style>
