# DjiModel：Update Time 2019/06/20 10:40 Version 1.0
## 项目使用前提 ##
  
1. 本项目测试机型为大疆的御pro 2，遥控器为不带屏遥控器。（据说带屏遥控器是无法使用大疆SDK的）
2. 集成使用要点：
	1. 需先测试手机下载大疆专用APP DJI GO 4进行无人机的对频操作，此时应该注意，每次将手机和无人机进行USB连接时，会弹出默认启动应用弹框，应选择仅次一次；不然后续装上自己的应用后，连上无人机是无法拉起我们自己的应用的。
	2. 当连接成功，可以正常起飞后，此时装入自己的应用，启动并执行 DJIModel中的 startRegisterSDK 方法，执行注册大疆SDK。
	3. 注册成功后，会弹出登录弹框，提示登录大疆账号，此时应填入自己的大疆账号（国内每三个月登录一次，国外不受此限制）
	4. `setDjiProductStateCallBack`是回调产品数据，当存在产品时，会执行`onAirPlaneStateChange（bool）`方法，传入参数为true
	5. 此时可以调用videoPreview方法，传入surfaceView，来执行无人机摄像头数据预览
	6. 也可以通过设置`setDjiVideoDataCallBack`回调，来获取无人机回传的H264数据，获取H264数据时，视频预览会静帧
	7. 通过`setAirPlaneCameraParam`方法，来设置无人机摄像头参数，具体参数请查看`DJICameraParams`类，其中有详细的说明
