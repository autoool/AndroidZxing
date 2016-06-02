## zxing 扫码

### zbarexample

- zbar官方示例

## zxbar

- zbar 打包编译[http://www.blackdogfoundry.com/blog/zbar-bar-code-qr-code-reader-android/]
- zbar 和 zxing 集成*[（Zbar 解码、ZXing 管理相机）](http://blog.csdn.net/b2259909/article/details/43273231)*[http://blog.csdn.net/b2259909/article/details/43273231#reply]
- ​待做的是把扫码提取成fragment 或者更方便回调的方式

### zxing-android-app

- 参考网址[http://stackoverflow.com/questions/8708705/how-to-use-zxing-in-android]
- [http://www.eoeandroid.com/thread-319592-1-1.html?_dsign=d1c8032d]

## zxing-android

- 参考[https://github.com/mitoyarzun/zxingfragmentlib]
- 原因：一直使用上面的库，在使用过程中发现一维码和二维码识别的并不很好，好像很距离有关。有时候很久扫不出来，一维码识别感觉没有二维码有效。但是使用Barcode Scan 这个app，一维码和二维码都很有效的识别，速度也很快。所以打算自己打一个包
- 提取成library  zxing 和 zbar 的混合，扫码速度很快 使用方法见app项目示例
- todo  扫码框样式调整

## app

- 测试项目

#### 截图

 ![device-2016-05-25-181756](screenshots\device-2016-05-25-181756.png)