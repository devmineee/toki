<script setup>
import { computed, onMounted, onUnmounted, reactive, ref,h,shallowReactive,watch } from 'vue'
import SvgIcon from '@jamescoyle/vue-icon'
import { mdiMicrophone, mdiVideo, mdiMonitorShare, mdiRadioboxMarked, mdiEmoticonOutline, mdiCog, mdiChatProcessingOutline } from '@mdi/js'
import RoomGameModal from '../modal/RoomGameModal.vue'
import { useWebSocket } from '@vueuse/core'
import kurentoUtil from 'kurento-utils';
import RoomUserView from '@/components/room_components/RoomUserView.vue'
import { toRaw } from 'vue'
// import { useTokiRoomStore } from '@/stores/tokiroom'
//import user from '@/stores/user'
import { watchEffect } from 'vue'
import { watchPostEffect } from 'vue'
import ScreenRecord from '@/components/screenRecord/ScreenRecord.vue'
import RoomMeetingModal from '../modal/RoomMeetingModal.vue';
import { useRouter } from 'vue-router'
import RoomSubMeetingModal from '@/components/modal/RoomSubMeetingModal.vue'
import RoomVoiceChangeModal from '@/components/modal/RoomVoiceChangeModal.vue'
import RoomVoteModal from '@/components/modal/RoomVoteModal.vue'
import RoomInviteModal from '@/components/modal/RoomInviteModal.vue'
import RoomConfigModal from '@/components/modal/RoomConfigModal.vue'
import RoomVideoBackgroundModal from '@/components/modal/RoomVideoBackgroundModal.vue'
import RoomVideoVirtualModal from '@/components/modal/RoomVideoVirtualModal.vue'
import RoomSettingModal from '@/components/modal/RoomSettingModal.vue'
import RoomChatComponent from '@/components/room_components/RoomChatComponent.vue'
////
// const testRooms = useTokiRoomStore();
//JiHoon Jung <mudokja@gmail.com>
const props = defineProps({
  userInfo: Object,
  roomInfo: Object
})
const userInfo = ref(props.userInfo)
const roomInfo = ref(props.roomInfo)

const recordedVideoElement = ref(null);
const previewCanvas = ref(null); // 미리 보기용 캔버스 요소의 참조
let previewCtx=ref('');


const tokiRoomMembers = ref([])
const tokiRoomVideo=ref({})
const wsUrl='wss://i10b205.p.ssafy.io/ws/room'
// const wsUrl='ws://localhost:8081/ws/room'
const { status, data, send, open, close } = useWebSocket(wsUrl, {
  heartbeat: {
    message: JSON.stringify({id:"ping",value:"ping"}),
    interval: 50000,
    pongTimeout:30000,
  },
  onConnected:()=> {
    console.log("연결됨")
  },
  onMessage: (ws,message) => {
    let parsedMessage = JSON.parse(message.data);
	switch (parsedMessage.id) {
    case 'existingParticipants':{
      
      createMember(userInfo.value.name);
      const receive=watchPostEffect(()=>{
        if(tokiRoomVideo.value[userInfo.value.name]){
          onExistingParticipants(parsedMessage)
        }
      })
      receive();
    }
      
		break;
    case 'newParticipantArrived':
		onNewParticipant(parsedMessage);
		break;
	case 'participantLeft':
		onParticipantLeft(parsedMessage);
		break;
	case 'receiveVideoAnswer':
		receiveVideoResponse(parsedMessage)
      break;
  case 'pong':
      break;

	case 'iceCandidate':
		toRaw(tokiRoomMembers.value.find(v=>v.name===parsedMessage.name)).rtcPeer.addIceCandidate(parsedMessage.candidate, function (error) {
      if (error) {
        console.error("Error adding candidate: " + error);
        return;
        }
    });
    break;
	default:
		console.error('Unrecognized message', parsedMessage);
	}
  }
})
const sendMessage=(message)=>{
    const msg=JSON.stringify(message)
    // console.dir(`message : ${msg}`)
    send(msg)
}

function register() {//room관련 정보 가져옴

	let message = {
		id : 'joinRoom',
		name : userInfo.value.name,
		room : roomInfo.value.roomPk,
	}
	sendMessage(message);
}
const start=()=>{
    register()
}
// watch(tokiRoomVideo, (newVal) => {
//   console.log("변화!",newVal.value)
// })


function onParticipantLeft(request) {//나머지 참가자 내보내기
	console.log('Participant ' + request.name + ' left');
	const participant =tokiRoomMembers.value.find(v=>v.name===request.name);//참가자 배열 받기
  participant.rtcPeer.dispose();
  tokiRoomMembers.value=tokiRoomMembers.value.filter(v => v.name !== request.name)
  console.log(tokiRoomMembers.value)
  delete tokiRoomVideo.value[request.name]
}

function receiveVideoResponse(result) {//참가자 비디오 응답 오는지 확인
  // console.log("받음")
  // console.dir(toRaw(tokiRoomMembers.value.find(v=>v.name===result.name).rtcPeer.processAnswer))
	toRaw(tokiRoomMembers.value.find(v=>v.name===result.name).rtcPeer).processAnswer (result.sdpAnswer, function (error) {
		if (error) {
      console.log("응답에러")
      return console.error (error);
    }
	});
}

function receiveVideo(sender) {
	let video = toRaw(tokiRoomVideo.value[sender]);
  const participant = tokiRoomMembers.value.find(v => v.name === sender)
    participant.video=video
	let options = {
    remoteVideo: video,
      onicecandidate:participant.onicecandidate.bind(participant)
      }
	participant.rtcPeer =new kurentoUtil.WebRtcPeer.WebRtcPeerRecvonly(options,
		function (error) {
            if(error) {
                return console.error(error);
            }
            this.generateOffer (participant.offerToReceiveVideo.bind(toRaw(participant)));
	});
}

const onExistingParticipants = async(msg) => {
    const constraints = {
    audio: true,
    video: {
      mandatory: {
        maxWidth: 320,
        maxFrameRate: 20,
        minFrameRate: 15
      }
    }
  }
  
  console.log(userInfo.value.name + " registered in room " + roomInfo.value.roomPk);

  const participant = tokiRoomMembers.value.find(v=>v.name===userInfo.value.name)
  participant.video=tokiRoomVideo.value[userInfo.value.name]
	let video = participant.video;
  /////////////////////////////////////////////////////////
let stream = await navigator.mediaDevices.getDisplayMedia({ video: true }) // 다른 비디오 소스로 변경하려면 적절한 constraints를 전달합니다.
    const videoElement = document.createElement('video');
    videoElement.srcObject = stream;    
   let audioStream =await navigator.mediaDevices.getUserMedia({ audio: true })
        // 오디오 스트림을 얻었습니다. 이제 이를 사용할 수 있습니다.
        console.log("오디오 스트림을 얻었습니다:", audioStream);

        // Web Audio API를 사용하여 오디오 스트림의 볼륨을 조절합니다.
        const audioContext = new AudioContext();
        const source = audioContext.createMediaStreamSource(audioStream);
        const gainNode = audioContext.createGain();
        const filter = audioContext.createBiquadFilter();//필터
        // 필터 유형을 피치 이동으로 설정
        //filter.type = "peaking";1
        // 중심 주파수 설정 (1000Hz를 기준으로 피치를 변경)
       // filter.frequency.value = 3000;
 
        // 피치 조절 (음성의 높낮이를 변경)
        //filter.detune.value = -100; // 100 cents (1 반음)만큼 올림
        // filter.detune.value = -100; // 100 cents (1 반음)만큼 내림

        // 오디오 소스와 필터 연결
        // source.connect(audioContext.destination);
        //filter.connect(gainNode);

        // 오디오 스트림에 gainNode 연결
        source.connect(gainNode);
        gainNode.connect(audioContext.destination);
        
        filter.type = "highshelf";
        //filter.frequency.setValueAtTime(1000, audioCtx.currentTime);
        //filter.gain.setValueAtTime(0, audioCtx.currentTime);
        
        // 볼륨 조절 
        gainNode.gain.value = 100; 
    // 캔버스 크기 설정
    // previewCanvas.value.width = stream.getVideoTracks()[0].getSettings().width;
    // previewCanvas.value.height = stream.getVideoTracks()[0].getSettings().height;

    // // 캔버스에 비디오 프레임 그리기
    // previewCtx.value = previewCanvas.value.getContext('2d');
    // // 미리 보기용 캔버스에 스트림 프레임을 그립니다.
    
    // videoElement.play();
    // videoElement.onplay = () => {
    //   const drawFrame = () => {
    //     previewCtx.value.drawImage(videoElement, 0, 0, previewCanvas.value.width, previewCanvas.value.height);
        
    //       requestAnimationFrame(drawFrame);
        
    //   };
    //   drawFrame();
    // };
    recordedVideoElement.value.srcObject = stream; // 미리 보기 비디오 요소에 스트림 설정
    recordedVideoElement.value.play(); // 비디오 재생

////////////////////////////////////////////////////////////////

        const options = {
            localVideo: video,
            videoStream: stream, // 새로운 비디오 스트림을 전달합니다.
            audioStream:audioContext.destination.stream,
            onicecandidate: participant.onicecandidate.bind(participant)
        };

        // WebRTC 피어 생성
        participant.rtcPeer = new kurentoUtil.WebRtcPeer.WebRtcPeerSendonly(options,
            function (error) {
                if (error) {
                    return console.error(error);
                }
                console.log("참가자", participant);
                this.generateOffer(participant.offerToReceiveVideo.bind(toRaw(participant)));
            });
    

	msg.data.forEach(participantBatch);
}
const onNewParticipant = (request) => {
  createMember(request.name)
  const receive=watchPostEffect(()=>{
    if(tokiRoomVideo.value[request.name]){
      receiveVideo(request.name)
    }
  })
  receive();
}
const participantBatch = (sender) => {
  createMember(sender)
  const receive=watchPostEffect(()=>{
    if(tokiRoomVideo.value[sender]){
      receiveVideo(sender)
    }
    receive();
})
}

const createMember = (userName) => {
  const tokiRoomMember =ref({
    name: userName,
    video: null,
    rtcPeer: null,
    onicecandidate: function(candidate){
            console.log("Local candidate" + JSON.stringify(candidate));
            const message = {
            id: 'onIceCandidate',
            candidate: candidate,
            name: userName
      };
            sendMessage(message);
    },
    offerToReceiveVideo:function(error, offerSdp){
		if (error) return console.error ("sdp offer error")
		const msg =  { id : "receiveVideoFrom",
				sender : userName,
				sdpOffer : offerSdp
			};
		sendMessage(msg);
	}
  })
  tokiRoomMembers.value= [...tokiRoomMembers.value,tokiRoomMember.value]
  
}
////



// 아이콘 변수
// const pathMicrophone = ref(mdiMicrophone )
const pathVideo = ref(mdiVideo)
const pathMonitorShare = ref(mdiMonitorShare)
const pathRadioboxMarked = ref(mdiRadioboxMarked)
const pathEmoticonOutline = ref(mdiEmoticonOutline)
const pathCog = ref(mdiCog)
const pathChatProcessingOutline  = ref(mdiChatProcessingOutline)

// 채팅 확장
const chatBox = ref(false)


const menuMicOpen = ref(false)
const menuVideoOpen = ref(false)
const menuShareOpen = ref(false)
const menuRecordOpen = ref(false)
const menuEmoticonOpen = ref(false)
const menuSettingOpen = ref(false)
const menuChatOpen = ref(false)

const roomGameDialog = ref(false)

const openGameDialog = () => {
  roomGameDialog.value = !roomGameDialog.value
}

const roomMeetingDialog = ref(false)

const oepnMeetingDialog = () => {
  roomMeetingDialog.value = !roomMeetingDialog.value
}

// 음성 변조 모달창 on / off
const roomVoiceChangeDialog = ref(false)
const openVoiceChangeDialog = () => {
  roomVoiceChangeDialog.value = !roomVoiceChangeDialog.value
}
// 비디오 on / off
const videoOnOff = ref(false)

// 가상 배경 모달창 on / off
const roomVideoBackgroundDialog = ref(false)
const openVideoBackgroundDialog = () => {
  roomVideoBackgroundDialog.value = !roomVideoBackgroundDialog.value
}

// 버츄얼 적용 모달창 on / off
const roomVideoVirtualDialog = ref(false)
const openVideoVirtualDialog = () => {
  roomVideoVirtualDialog.value = !roomVideoVirtualDialog.value
}

// 화면 공유 on / off
const shareOnOff = ref(false)

// 녹화  on / off
const recordOnOff = ref(false)

// 이모티콘 클릭 할 때, 함수
const selectEmoticon = (emt) => {
  console.log(`${emt} 출력`)
}

// 투표 모달창 on / off
const roomVotegDialog = ref(false)
const openVoteDialog = () => {
  roomVotegDialog.value = !roomVotegDialog.value
}

// 초대 모달창 on / off
const roomInviteDialog = ref(false)
const openInvitDialog = () => {
  roomInviteDialog.value = !roomInviteDialog.value
}

// 환경설정 모달창 on / off
const roomConfigDialog = ref(false)
const openConfigDialog = () => {
  roomConfigDialog.value = !roomConfigDialog.value
}

// 나가기 버튼 클릭 시, 함수
const router = useRouter()
const exitRoom = () => {
  sessionStorage.removeItem('roomData')
  router.push({ name: 'home' })
}

const roomSettingDialog = ref(false)
const openRoomSettingDialog = () => {
  roomSettingDialog.value = !roomSettingDialog.value
}

const screenWidth = ref(window.innerWidth)

function handleResize() {
  screenWidth.value = window.innerWidth
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  start()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})





const isLagerScreen = computed(() => screenWidth.value >= 1280)

const colSize = computed(() => isLagerScreen.value ? 2 : 1)
const colOffset = computed(() => isLagerScreen.value ? 0 : 1)

</script>
<template>
   <div>
    <video ref="recordedVideoElement" controls style="max-width: 100%; " width="500" height="300"></video>
    <!-- <canvas ref="previewCanvas" controls style="max-width: 100%;" width="100" height="60"></canvas> -->
  </div>    
  <v-container id="enter" class="h-100" style="min-width: 1400px">
    
    <v-row class="h-100">
      <!-- 화면 -->
      <v-col id="screen">
        <!-- 위 서브 화면 -->
        <v-row class="mt-1" style="height: 20%;">
          <v-col v-for="(member,key,index) in tokiRoomMembers" :id="`biggBG${index}`" :key="member.name" class="ml-2 mr-2 mb-2 d-flex align-end">
            <RoomUserView  :userInfo="member">
              <template #video>
                <video :id="`room-${member.name}`" class="toki-video" :ref="(el)=>(tokiRoomVideo[member.name]=el)" autoplay></video>
              </template>
            </RoomUserView>
          </v-col>
        </v-row>
        <!-- 메인 화면 -->
        <v-row id="main-screen" class="h-50">
          <v-col cols="12">
            메인 화면
          </v-col>
        </v-row>
        <!-- 아래 서브 화면 -->
        <v-row class="mb-1" style="height: 20%;">
          <v-col id="biggBG1" class="ml-2 mr-2 mt-2 d-flex align-end">
            <v-sheet>아이디 or 닉네임</v-sheet>
          </v-col>
          <v-col id="biggBG2" class="ml-2 mr-2 mt-2 d-flex align-end">
            <v-sheet>아이디 or 닉네임</v-sheet>
          </v-col>
          <v-col id="biggBG3" class="ml-2 mr-2 mt-2 d-flex align-end">
            <v-sheet>아이디 or 닉네임</v-sheet>
          </v-col>
          <v-col id="biggBG4" class="ml-2 mr-2 mt-2 d-flex align-end">
            <v-sheet>아이디 or 닉네임</v-sheet>
          </v-col>
          <v-col id="biggBG5" class="ml-2 mr-2 mt-2 d-flex align-end">
            <v-sheet>아이디 or 닉네임</v-sheet>
          </v-col>
        </v-row>


       <!-- 영상 옵션 바 -->
       <v-row class="mt-1 ml-1 mr-1" >
          <v-col
            justify="center"
            :cols="colSize"
            :offset="colOffset"
          >
            <div class="mx-4 hidden-sm-and-down"></div>

            <v-menu location="top" v-model="menuMicOpen">
              <template v-slot:activator="{ props }">
                <v-btn
                  v-if="isLagerScreen"
                  :prepend-icon="micOnOff? 'mdi-microphone' : 'mdi-microphone-off'"
                  :append-icon="menuMicOpen ? 'mdi-menu-down' : 'mdi-menu-up'"
                  :color="micOnOff? 'green-lighten-1' : 'black'"
                  class="ma-2"
                  v-bind="props"
                  size="x-large"
                  style="border-radius: 30px; border: 1px solid white;"
                >
                  마이크
                </v-btn>
                <v-btn
                  v-else
                  class="ma-2"
                  :color="micOnOff? 'green-lighten-1' : 'black'"
                  :icon="micOnOff? 'mdi-microphone' : 'mdi-microphone-off'"
                  v-bind="props"
                  width="100%"
                  size="large"
                  style="border: 1px solid white;"
                >
                </v-btn>
              </template>
              <v-list bg-color="black" style="text-align: center;">
                <v-list-item 
                  link 
                  value="마이크-on-off"
                  @click="micOnOff = !micOnOff"
                >
                  마이크 on/off
                </v-list-item>

                <v-list-item 
                  link 
                  value="음성변조"
                  @click="openVoiceChangeDialog"
                >
                  음성 변조
                </v-list-item>
              </v-list>
            </v-menu>
          </v-col>


          <!-- 캠 설정 -->
          <!-- <v-col>
            <v-expansion-panels>
              <v-expansion-panel bg-color="black" style="border-radius: 30px; border: 1px solid white;">
                <v-expansion-panel-title expand-icon="mdi-menu-down">
                  <svg-icon type="mdi" :path="pathVideo"></svg-icon>
                  <p class="ml-5">캠</p>
                </v-expansion-panel-title>
                <v-expansion-panel-text>
                  <v-list bg-color="black">
                    <v-list-item value="캠-on-off">캠 on/off</v-list-item>
                    <v-list-item value="가상배경">가상 배경</v-list-item>
                    <v-list-item value="버츄얼">버츄얼 적용</v-list-item>
                  </v-list>
                </v-expansion-panel-text>
              </v-expansion-panel>
            </v-expansion-panels>
          </v-col> -->

          <v-col
            justify="center"
            :cols="colSize"
            :offset="colOffset"
          >
            <div class="mx-4 hidden-sm-and-down"></div>

            <v-menu location="top" v-model="menuVideoOpen">
              <template v-slot:activator="{ props }">
                <v-btn
                  v-if="isLagerScreen"
                  :prepend-icon="videoOnOff? 'mdi-video' : 'mdi-video-off'"
                  :append-icon="menuVideoOpen ? 'mdi-menu-down' : 'mdi-menu-up'"
                  :color="videoOnOff? 'green-lighten-1' : 'black'"
                  class="ma-2 "
                  v-bind="props"
                  size="x-large"
                  style="border-radius: 30px; border: 1px solid white;"
                >
                  캠
                </v-btn>
                <v-btn
                  v-else
                  class="ma-2"
                  :color="videoOnOff? 'green-lighten-1' : 'black'"
                  :icon="videoOnOff? 'mdi-video' : 'mdi-video-off'"
                  v-bind="props"
                  size="large"
                  style="border: 1px solid white;"
                >

                </v-btn>
              </template>
              <v-list bg-color="black" style="text-align: center;">
                <v-list-item link value="캠-on-off" @click="videoOnOff = !videoOnOff">
                캠 on/off
                </v-list-item>

                <v-list-item link value="가상배경" @click="openVideoBackgroundDialog">
                  가상 배경
                </v-list-item>

                <v-list-item link value="버츄얼" @click="openVideoVirtualDialog">
                  버츄얼 적용
                </v-list-item>
              </v-list>
            </v-menu>
          </v-col>

          <!-- 화면 공유 -->
          <!-- <v-col>
            <v-expansion-panels>
              <v-expansion-panel bg-color="black" style="border-radius: 30px; border: 1px solid white;">
                <v-expansion-panel-title expand-icon="mdi-menu-down">
                  <svg-icon type="mdi" :path="pathMonitorShare"></svg-icon>
                  <p class="ml-5">공유</p>
                </v-expansion-panel-title>
                <v-expansion-panel-text>
                  <v-list bg-color="black">
                    <v-list-item value="화면공유">화면 공유하기</v-list-item>
                  </v-list>
                </v-expansion-panel-text>
              </v-expansion-panel>
            </v-expansion-panels>
          </v-col> -->

          <v-col
            justify="center"
            :cols="colSize"
            :offset="colOffset"
          >
            <div class="mx-4 hidden-sm-and-down"></div>

            <v-menu location="top" v-model="menuShareOpen">
              <template v-slot:activator="{ props }">
                <v-btn
                  v-if="isLagerScreen"
                  :prepend-icon="shareOnOff? 'mdi-monitor-share' : 'mdi-monitor-off'"
                  :append-icon="menuShareOpen ? 'mdi-menu-down' : 'mdi-menu-up'"
                  :color="shareOnOff? 'red-lighten-1' : 'black'"
                  class="ma-2 "
                  v-bind="props"
                  size="x-large"
                  style="border-radius: 30px; border: 1px solid white;"
                >
                  공유
                </v-btn>
                <v-btn
                  v-else
                  class="ma-2"
                  :color="shareOnOff? 'red-lighten-1' : 'black'"
                  :icon="shareOnOff? 'mdi-monitor-share' : 'mdi-monitor-off'"
                  v-bind="props"
                  size="large"
                  style="border: 1px solid white;"
                >

                </v-btn>
              </template>
              <v-list bg-color="black" style="text-align: center;">                  
                <v-list-item value="화면공유" @click="shareOnOff = !shareOnOff">
                  화면 공유하기
                </v-list-item>
              </v-list>
            </v-menu>
          </v-col>


          <!-- 녹화 -->
          <!-- <v-col>
            <v-expansion-panels>
              <v-expansion-panel bg-color="black" style="border-radius: 30px; border: 1px solid white;">
                <v-expansion-panel-title expand-icon="mdi-menu-down">
                  <svg-icon type="mdi" :path="pathRadioboxMarked"></svg-icon>
                  <p class="ml-5">녹화</p>
                </v-expansion-panel-title>
                <v-expansion-panel-text>
                  <v-list bg-color="black">
                    <v-list-item value="화면녹화">화면 녹화</v-list-item>
                  </v-list>
                </v-expansion-panel-text>
              </v-expansion-panel>
            </v-expansion-panels>
          </v-col> -->

          <v-col
            justify="center"
            :cols="colSize"
            :offset="colOffset"
          >
            <div class="mx-4 hidden-sm-and-down"></div>

            <v-menu location="top" v-model="menuRecordOpen">
              <template v-slot:activator="{ props }">
                <v-btn
                  v-if="isLagerScreen"
                  prepend-icon="mdi-radiobox-marked"
                  :append-icon="menuRecordOpen ? 'mdi-menu-down' : 'mdi-menu-up'"
                  :color="recordOnOff? 'red-lighten-1' : 'black'"
                  class="ma-2 "
                  v-bind="props"
                  size="x-large"
                  style="border-radius: 30px; border: 1px solid white;"
                >
                  녹화
                </v-btn>
                <v-btn
                  v-else
                  class="ma-2"
                  :color="recordOnOff? 'red-lighten-1' : 'black'"
                  v-bind="props"
                  icon="mdi-radiobox-marked"
                  size="large"
                  style="border: 1px solid white;"
                >

                </v-btn>
              </template>
              <v-list bg-color="black" style="text-align: center;">                  
                <v-list-item value="화면녹화" @click="recordOnOff = !recordOnOff">
                  화면 녹화
                </v-list-item>
              </v-list>
            </v-menu>
          </v-col>


          <!-- 이모티콘 -->
          <!-- <v-col cols="1" sm="2">
            <v-expansion-panels>
              <v-expansion-panel bg-color="black" style="border-radius: 30px; border: 1px solid white;">
                <v-expansion-panel-title expand-icon="mdi-menu-down">
                  <svg-icon type="mdi" :path="pathEmoticonOutline"></svg-icon>
                  <p class="ml-5">이모티콘</p>
                </v-expansion-panel-title>
                <v-expansion-panel-text>
                  <v-chip-group>
                    <v-chip value="화면녹화">😀</v-chip>
                    <v-chip value="화면녹화">😀</v-chip>
                    <v-chip value="화면녹화">😀</v-chip>
                    <v-chip value="화면녹화">😀</v-chip>
                    <v-chip value="화면녹화">😀</v-chip>
                  </v-chip-group>
                </v-expansion-panel-text>
              </v-expansion-panel>
            </v-expansion-panels>
          </v-col> -->

          <v-col
            justify="center"
            cols="1"
          >
            <div class="mx-4 hidden-sm-and-down"></div>

            <v-menu location="top" v-model="menuEmoticonOpen">
              <template v-slot:activator="{ props }">
                <v-btn
                  class="ma-2"
                  color="black"
                  v-bind="props"
                  icon="mdi-emoticon-outline"
                  size="large"
                  style="border: 1px solid white;"
                >

                </v-btn>
              </template>              
              <v-list bg-color="black" style="text-align: center;">                  
                <v-list-item-group multiple>
                  <v-row>
                    <v-col cols="12">
                      <v-chip-group row>
                        <v-chip value="화면녹화">😀</v-chip>
                        <v-chip value="화면녹화">😀</v-chip>
                        <v-chip value="화면녹화">😀</v-chip>
                        <v-chip value="화면녹화">😀</v-chip>
                      </v-chip-group>
                    </v-col>
                  </v-row>
                  <v-row>
                    <v-col cols="12">
                      <v-chip-group row>
                        <v-chip value="화면녹화">😀</v-chip>
                        <v-chip value="화면녹화">😀</v-chip>
                        <v-chip value="화면녹화">😀</v-chip>
                        <v-chip value="화면녹화">😀</v-chip>
                      </v-chip-group>
                    </v-col>
                  </v-row>
                  <!-- <v-chip value="화면녹화">😀</v-chip>
                  <v-chip value="화면녹화">😀</v-chip>
                  <v-chip value="화면녹화">😀</v-chip>
                  <v-chip value="화면녹화">😀</v-chip>
                  <v-chip value="화면녹화">😀</v-chip> -->
                </v-list-item-group>
              </v-list>
            </v-menu>
          </v-col>

          <!-- 환경 설정 -->
          <!-- <v-col>
            <v-expansion-panels>
              <v-expansion-panel bg-color="black" style="border-radius: 10px; border: 1px solid white;">
                <v-expansion-panel-title hide-actions>
                  <svg-icon type="mdi" :path="pathCog"></svg-icon>
                  <p class="ml-5">설정</p>
                </v-expansion-panel-title>
              </v-expansion-panel>
            </v-expansion-panels>
          </v-col> -->
          <v-col
            justify="center"
            cols="1"
          >
            <div class="mx-4 hidden-sm-and-down"></div>

            <v-menu location="top" v-model="menuSettingOpen">
              <template v-slot:activator="{ props }">
                <v-btn
                  class="ma-2"
                  color="black"
                  v-bind="props"
                  icon="mdi-cog"
                  size="large"
                  style="border: 1px solid white;"
                >

                </v-btn>
              </template>
              <v-list bg-color="black" style="text-align: center; width: 250px; white-space: nowrap;">   
                <v-list-item 
                  prepend-icon="mdi-gamepad-variant"
                  style="margin-left: 10px;"
                  value="게임"
                  @click="openGameDialog"
                >
                  게임
                </v-list-item>
                
                <v-list-item 
                  prepend-icon="mdi-account-group"
                  style="margin-left: 10px;"
                  value="소회의실"
                  @click="oepnMeetingDialog"
                >
                  소회의실
                </v-list-item>
                <v-list-item 
                  prepend-icon="mdi-vote"
                  style="margin-left: 10px;"
                  value="투표"
                  @click="openVoteDialog"
                >
                  투표
                </v-list-item>
                <v-list-item 
                  prepend-icon="mdi-send"
                  style="margin-left: 10px;"
                  value="초대"
                  @click="openInvitDialog"
                >
                  초대
                </v-list-item>
                <v-list-item 
                  prepend-icon="mdi-rabbit"
                  style="margin-left: 10px;"
                  value="방정보변경"
                  @click="openRoomSettingDialog"
                >
                  방 정보 변경
                </v-list-item>
                <v-list-item 
                  prepend-icon="mdi-cog"
                  style="margin-left: 10px;"
                  value="환경설정"
                  @click="openConfigDialog"
                >
                  환경 설정
                </v-list-item>
                <v-list-item 
                  prepend-icon="mdi-exit-to-app"
                  style="margin-left: 10px;"
                  value="나가기"
                  @click="exitRoom"
                >
                  방 나가기
                </v-list-item>
              </v-list>
              
            </v-menu>
          </v-col>
          
          
          <!-- 채팅 -->
          <v-col
            justify="center"
            :cols="colSize"            
          >
            <!-- <div class="mx-4 hidden-sm-and-down"></div> -->
          
            <v-btn
              v-if="isLagerScreen"
              prepend-icon="mdi-chat-processing-outline"
              color="black"
              class="ma-2 "
              v-bind="props"
              size="x-large"
              style="border-radius: 10px; border: 1px solid white;"
              @click="chatBox = !chatBox"
            >
              채팅
            </v-btn>
              
            <v-btn
              v-else
              class="ma-2"
              color="black"
              v-bind="props"
              icon="mdi-chat-processing-outline"
              size="large"
              style="border-radius: 10px; border: 1px solid white;"
              
            >

            </v-btn>
              

            
          </v-col>


          <!-- <v-col>
            <v-expansion-panels @click="chatBox = !chatBox">
              <v-expansion-panel bg-color="black" style="border-radius: 10px; border: 1px solid white;">
                <v-expansion-panel-title collapse-icon="mdi-rabbit-variant-outline" expand-icon="mdi-menu-right">
                  <svg-icon type="mdi" :path="pathChatProcessingOutline"></svg-icon>
                  <p>채팅</p>
                </v-expansion-panel-title>
              </v-expansion-panel>
            </v-expansion-panels>
          </v-col> -->
        </v-row>

        <!-- 영상 타이틀 -->
        <v-row>
          <v-col cols="2">
            <v-sheet>닉네임</v-sheet>
          </v-col>
          <v-col cols="2">
            <v-sheet>화상 채팅 방 제목</v-sheet>
          </v-col>
        </v-row>
      </v-col>

      
      <!-- 채팅 화면 -->
      <v-col v-if="chatBox" id="chatt" cols="3">
        <v-row class="h-100">
          <v-col><RoomChatComponent/></v-col>
        </v-row>
      </v-col>
    </v-row>



    <!-- Modal 화면 -->
    <RoomGameModal 
      :roomGameDialog="roomGameDialog"
      @update:roomGameDialog="roomGameDialog = $event"
    />

    <RoomMeetingModal
      :roomMeetingDialog="roomMeetingDialog"
      @update:roomMeetingDialog="roomMeetingDialog = $event"
    />
    <!-- Modal (투표) 화면 -->
    <RoomVoteModal
      :roomVotegDialog="roomVotegDialog"
      @update:roomVotegDialog="roomVotegDialog = $event"
    />
    <!-- Modal (초대) 화면 -->
    <RoomInviteModal
      :roomInviteDialog="roomInviteDialog"
      @update:roomInviteDialog="roomInviteDialog = $event"
    />
    <!-- Modal (환경 설정) 화면 -->
    <RoomConfigModal
      :roomConfigDialog="roomConfigDialog"
      @update:roomConfigDialog="roomConfigDialog = $event"
    />
    <!-- Modal (음성 변조) 화면 -->
    <RoomVoiceChangeModal
      :roomVoiceChangeDialog="roomVoiceChangeDialog"
      @update:roomVoiceChangeDialog="roomVoiceChangeDialog = $event"
    />
    <!-- Modal (가상 배경) 화면 -->
    <RoomVideoBackgroundModal
      :roomVideoBackgroundDialog="roomVideoBackgroundDialog"
      @update:roomVideoBackgroundDialog="roomVideoBackgroundDialog = $event"
    />
    <!-- Modal (버츄얼) 화면 -->
    <RoomVideoVirtualModal
      :roomVideoVirtualDialog="roomVideoVirtualDialog"
      @update:roomVideoVirtualDialog="roomVideoVirtualDialog = $event"
    />

    <RoomSettingModal 
      :roomSettingDialog="roomSettingDialog"
      @update:roomSettingDialog="roomSettingDialog = $event"
    />
  </v-container>
  <link href="https://cdn.jsdelivr.net/npm/@mdi/font@5.x/css/materialdesignicons.min.css" rel="stylesheet">
</template>
<style scoped>
#main-screen {
  background-color: rgb(167, 111, 169);
}

#screen {
  background-color: rgb(0, 0, 0);
  white-space: nowrap;
}
#chatt {
  background-color: aqua;
}

.toki-video{
  width: 100%;
	height: auto;
}

/* 작은 화면 */
#biggBG1 {
  background-color: antiquewhite;
}
#biggBG2 {
  background-color: aquamarine;
}
#biggBG3 {
  background-color: rgb(186, 222, 176);
}
#biggBG4 {
  background-color: rgb(232, 255, 127);
}
#biggBG5 {
  background-color: rgb(255, 163, 127);
}

</style>