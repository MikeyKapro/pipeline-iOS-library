import groovy.json.JsonOutput
import org.apache.commons.collections4.map.LinkedMap

class MessageCard {
    Boolean needQRCode;        // 是否需要ipa的二维码
    String currentVersion;    // 当前版本号
    String currentBranch;    // 当前分支名
    String buildChannel;    // 构建渠道：手动、webhook
    String buildNum;       // 当前Build号
    String qrCodeUrl;     // 二维码URL
    String ipaUrl;       // IPA下载链接
    String xcodeConfiguration;      // 构建渠道：QA、TestFlight、Enterprise
    String installUrl; // 安装链接
}

@NonCPS
/**
* channel:构建渠道
* qrcodeUrl:能扫码下载APP的二维码链接
*/
// def getChanges(String currentVersion, String buildNum, String currentBranch, String channel, String qrcodeUrl) {
//     def changeLogSets = currentBuild.changeSets
//     List<String> result = ["### 构建成功，请删除已安装的App再用原生相机扫码安装\n"] as String[]
//     result += "#### 当前版本：${currentVersion}(${buildNum})\n"
//     result += "#### 当前分支：${currentBranch}\n"
//     result += "#### 触发方式：${channel}\n"
//     int total = 0
//     int maxNum = 10

//     for (int i = 0; i < changeLogSets.size(); i++) {
//         def entries = changeLogSets[i].items
//         for (int j = 0; j < entries.length; j++) {
//          if(total >= maxNum) {
//              continue
//          }
//             total = total + 1
//             def entry = entries[j]
//             String changeLog = "${entry.author}:${entry.msg}\n"
//             result += changeLog
//         }
//     }

//     if(total > maxNum) {
//      int diff = total - maxNum
//      result += "......剩余${diff}条提交信息\n"
//     }

//     result += "![screenshot](${qrcodeUrl})"
//     return result
// }

// 将标题转成发送给Microsoft的json需要的格式
def formatMicrosoftTitle(String title) {
    def items = [['type':'TextBlock', 'text':title, 'size':'large', 'wrap':true]]
    def result = ['type':'Container', 'style':'emphasis', 'items':items]
    return result
}

// 将触发构建的信息转成发送给Microsoft的json需要的格式
def formatMicrosoftTriggerInfo(MessageCard message) {
    String currentVersion = message.currentVersion + "(${message.buildNum})"
    def infoArray = [
    ['title':'当前版本', 'value':currentVersion],
    ['title':'当前分支', 'value':message.currentBranch],
    ['title':'触发方式', 'value':message.buildChannel],
    ['title':'渠道', 'value':message.xcodeConfiguration]]

    def items = [['type':'FactSet', 'spacing':'large', 'facts':infoArray]]
    def padding = ['left':'padding', 'right':'padding']
    def result = ['type':'Container', 'padding':padding, 'items':items]
    return result
}

// 将构建失败的信息转成发送给Microsoft的json需要的格式
def formatMicrosoftTriggerFailureInfo() {
    def infoArray = [['title':'构建名称', 'value':"${env.JOB_NAME}"],
                     ['title':'构建序号', 'value':"${env.BUILD_NUMBER}"]]

    def items = [['type':'FactSet', 'spacing':'large', 'facts':infoArray]]
    def padding = ['left':'padding', 'right':'padding']
    def result = ['type':'Container', 'padding':padding, 'items':items]
    return result
}

def formatMicrosoftTextBlock(String text) {
    return ['type':'TextBlock', 'text':text, 'wrap':true]
}

// 将触提交记录转成发送给Microsoft的json需要的格式
def formatMicrosoftChanges(String qrCodeUrl, Boolean needQRCode) {
    int total = 0
    int maxNum = 10 // 提交记录最多显示数
    def items = []
    def changeLogSets = currentBuild.changeSets
    for (int i = 0; i < changeLogSets.size(); i++) {
        def entries = changeLogSets[i].items
        for (int j = entries.length - 1; j >= 0; j--) {
            total = total + 1
            if(total > maxNum) { continue }
            def entry = entries[j]
            String changeLog = "${entry.author}:${entry.msg}"
            items << formatMicrosoftTextBlock(changeLog)
        }
    }

    // 超过10条不展示
    if(total > maxNum) {
        int diff = total - maxNum
        items << formatMicrosoftTextBlock("......剩余${diff}条提交信息")
    }

    if (needQRCode) {
        def qrCodeMap = ['type':'Image', 'url':qrCodeUrl]
        items << qrCodeMap
    }

    def padding = ['left':'padding', 'right':'padding']
    def result = ['type':'Container', 'padding':padding, 'items':items]
    return result
}

// 将点击事件转成发送给Microsoft的json需要的格式
def formatMicrosoftActions(String installUrl, String ipaUrl) {
    String installUrlText = '[直接安装]' + "(${installUrl})"
    def installUrlItems = [['type':'TextBlock', 'text':installUrlText, 'color':'Accent']]
    def installUrlColumn = ['type':'Column', 'items':installUrlItems, 'width':'auto']

    def ipaUrlText = '[下载IPA]' + "(${ipaUrl})"
    def ipaUrllItems = [['type':'TextBlock', 'text':ipaUrlText, 'color':'Accent']]
    def ipaUrlColumn = ['type':'Column', 'items':ipaUrllItems, 'width':'stretch']

    def items = [['type':'ColumnSet', 'columns':[installUrlColumn, ipaUrlColumn]]]
    def padding = ['left':'Small', 'right':'Default', 'top':'Small', 'bottom':'Small']
    def result = ['type':'Container', 'padding':padding, 'items':items]
    return result
}

// 构建失败时的点击事件
def formatMicrosoftFailureActions() {
    String buildUrlText = '[Jenkins链接]' + "(${env.BUILD_URL})"
    def buildUrlItems = [['type':'TextBlock', 'text':buildUrlText, 'color':'Accent']]
    def buildUrlColumn = ['type':'Column', 'items':buildUrlItems, 'width':'auto']
    def items = [['type':'ColumnSet', 'columns':[buildUrlColumn]]]
    def padding = ['left':'Small', 'right':'Default', 'top':'Small', 'bottom':'Small']
    def result = ['type':'Container', 'padding':padding, 'items':items]
}

def formatMicrosoftMessage(List body) {
    def content = ['type':'AdaptiveCard', 'version':'1.0', 'padding':'none', 'body':body]
    def attachments = [['contentType':'application/vnd.microsoft.card.adaptive', 'contentUrl':null, 'content':content]]
    def message = ['type':'message', 'attachments':attachments]
    def result = JsonOutput.toJson(message)
    return result
}

// 构建成功时向Microsoft发送的信息
def getMicrosoftTeamsBody(MessageCard message, String appName) {
    String activityTitle = message.needQRCode ? "构建成功，请删除所有已安装的${appName}再用原生相机扫码安装" : "构建成功"
    def titlePart = formatMicrosoftTitle(activityTitle)
    def triggerInfoPart = formatMicrosoftTriggerInfo(message)
    def changeLogPart = formatMicrosoftChanges(message.qrCodeUrl, message.needQRCode)
    def actionPart = formatMicrosoftActions(message.installUrl, message.ipaUrl)
    def body = [titlePart, triggerInfoPart, changeLogPart, actionPart]
    def result = formatMicrosoftMessage(body)
    return result
}

// 构建失败时向Microsoft发送的信息
def getMicrosoftTeamsFailureBody() {
    def titlePart = formatMicrosoftTitle("构建失败，请联系开发人员处理")
    def triggerInfoPart = formatMicrosoftTriggerFailureInfo()
    def actionPart = formatMicrosoftFailureActions()
    def body = [titlePart, triggerInfoPart, actionPart]
    def result = formatMicrosoftMessage(body)
    return result
}

/*
    发送通知给MicrosoftTeams
    webhookUrl 目标通知对象的webhookUrl
    body 发送的内容，json字符串格式
*/
def sendNotificationToMicrosoftTeams(String webhookUrl, String body) {
    def response = httpRequest acceptType: 'APPLICATION_JSON', httpMode: 'POST', requestBody:body, url: webhookUrl, customHeaders: [[name: 'Content-Type', value: 'application/json;charset=UTF-8']]
}

/*
    寻找Xcode中的configurationId
    xcodeConfiguration 打包选择的configuration
    buildConfigurationListId Xcode中configurationId的列表Id
    pbxprojPath xxx.pbxproj的路径
*/
def findBuildConfigurationId(String xcodeConfiguration, String buildConfigurationListId, String pbxprojPath) {
    String result = ""
    int index = 0
    while(result == "") {
        String buildConfigurationId = ""
        try {
            buildConfigurationId = sh(script:"""/usr/libexec/PlistBuddy -c "print objects:${buildConfigurationListId}:buildConfigurations:${index}" ${pbxprojPath}""", returnStdout: true).trim()
        }
        catch(Exception e) {
            println "*****\\(╯-╰)/*****不存在此configuration*****\\(╯-╰)/*****"
            throw e;
        }

        String matchConfiguration = sh(script:"""/usr/libexec/PlistBuddy -c "print objects:${buildConfigurationId}:name" ${pbxprojPath}""", returnStdout: true).trim()
        println "查询到的configurationId为：${buildConfigurationId}，对应的名称：${matchConfiguration}"
        if(matchConfiguration == xcodeConfiguration) {
            result = buildConfigurationId
            return result;
        }
        
        ++index
    }
}

// 获取失败后的信息
def getErrorMessageCard(String currentVersion, String buildNum, String currentBranch, String buildChannel, String xcodeConfiguration) {
    MessageCard messageCard = new MessageCard();
    messageCard.buildChannel = buildChannel;
    messageCard.currentVersion = currentVersion;
    messageCard.buildNum = buildNum;
    messageCard.currentBranch = currentBranch;
    messageCard.xcodeConfiguration = xcodeConfiguration;
    return messageCard
}

def getWebhookUrl(LinkedMap notificationMap, String notificationType) {
    String webhookUrl = notificationMap[notificationType][1]
    if (webhookUrl.isEmpty()) {
        error "*****\\(╯-╰)/*****缺少发布信息位置，请在配置文件notificationMap中添加*****\\(╯-╰)/*****"
    }
    return webhookUrl
}

// 打印阶段开始
def printStageStart(String message) {
    println "*****\\^o^/*****${message}阶段开始*****\\^o^/*****"
}

/*
    拼接IPA名称
    version 版本号
    build Build号
    configuration Xcode里配置的configuration
    enterpriseName 配置中enterprise的名称
    certificate 企业签提供的证书名
    modifyBuildNumToTime 是否将build号修改为构建时间
*/
def getIPAName(String version, String build, String configuration, String enterpriseName, String certificate, String appName, Boolean modifyBuildNumToTime) {
    String result = "${appName}${version}-${build}-"
    if(configuration == enterpriseName) {
        result = result + certificate
    } else {
        result = result + configuration
    }

    if (!modifyBuildNumToTime) {
        long timeStamp = System.currentTimeMillis();
        result = result + timeStamp
    }
    result = result + ".ipa"
    return result
}

// 字符串列表转换成带回车的字符串
def addEnterSymbol(List<String> list) {
    String result = ''
    for(element in list) {
        result = result + element + '\n'
    }
    return result.trim()
}

def call(Map map, LinkedMap configurationMap, LinkedMap notificationMap, String enterpriseName, List enterpriseCertificateList) {
properties([
    parameters([
        [$class: 'ChoiceParameter', 
            choiceType: 'PT_SINGLE_SELECT', 
            description: '项目打包的渠道，对应Xcode中的configuration\n' + "${configurationMap.values().collect { it[0] }.join('\n')}", 
            name: 'XcodeConfiguration', 
            randomName: 'choice-parameter-1', 
            script: [
                $class: 'GroovyScript', 
                fallbackScript: [
                    classpath: [], 
                    sandbox: false, 
                    script: 
                        'return[\'无法获取XcodeConfiguration\']'
                ], 
                script: [
                    classpath: [], 
                    sandbox: false, 
                    script: 
                        "return[${configurationMap.keySet().collect { "\"$it\"" }.join(',')}]".toString()
                ]
            ]
        ],
        [$class: 'CascadeChoiceParameter', 
            choiceType: 'PT_SINGLE_SELECT', 
            description: "APP签名用的证书名，当XcodeConfiguration为${enterpriseName}时影响IPA的名字，其余情况不影响", 
            name: 'Certificate', 
            randomName: 'choice-parameter-2', 
            referencedParameters: 'XcodeConfiguration',
            script: [
                $class: 'GroovyScript', 
                fallbackScript: [
                    classpath: [], 
                    sandbox: false, 
                    script: 
                        'return[\'无法获取Certificate\']'
                ], 
                script: [
                    classpath: [], 
                    sandbox: false, 
                    script: 
                        """if (XcodeConfiguration == \"${enterpriseName}\") {
  return[${enterpriseCertificateList.collect { "\"$it\"" }.join(',')}]
}""".toString()
                ]
            ]
        ]
    ])
])

    pipeline {
        agent any
        environment {
            // 默认打包出来的ipa名字
            defaultIpaName = ""
            // 触发构建的分支名
            branchName = ""
            // 触发构建的渠道，目前有webhook和手动两种
            channelMsg = ""
            // 当前版本号
            currentVersion = ""
            // 当前Build号
            currentBuildNum = ""
            // xcode中配置的configuration
            xcodeConfiguration = ""
            // OSS命令工具的路径
            ossutilmac = "${map.ossUploadToolPath}"
            // 上传OSS的主路径
            uploadOSSMainPath = "${map.uploadOSSMainPath}"
            // 根据渠道不同而不同的上传OSS的路径
            uploadOSSPath = ""
            // 下载OSS的主路径，也是图片存放的路径
            downloadOSSImageMainPath = "${map.downloadOSSImageMainPath}"
            // 下载ipa的主路径
            downloadOSSIPAMainPath = ""
            // 不同渠道的OSS上的存放文件夹的名称
            ossStoreFile = ""
            // IPA包的下载路径
            ipaDownloadUrl = ""
            // ipa包名
            ipaName =  ""

            // manifest文件的名字
            manifestName = "manifest.plist"
            // 是否需要ipa的二维码
            needQRCode = false
            // 二维码图片名
            qrCodeImageName = "iOSAppDownload.png"
            // 二维码中的信息
            qrCodeInfo = ""
            // project.pbxproj的路径
            String pbxprojPath = ""
            // configurationId
            String buildConfigurationId = ""
        }

        parameters {
            choice(name: 'Notification', choices: "${notificationMap.keySet().join('\n')}", description: '发布信息位置')
            string(name: 'ExpectVersion', defaultValue: '', description: '如果想要构建某个版本号的包则填写此项，不填则默认使用原有的版本号')
            string(name: 'ExpectBuild', defaultValue: '', description: '如果想要构建某个Build号的包则填写此项，不填则默认使用原有的Build号')
        }

        options {
                disableConcurrentBuilds()
                timeout(time: 1, unit: 'HOURS')
                //保持构建的最大个数
                buildDiscarder(logRotator(numToKeepStr: '7', daysToKeepStr: '7'))
        }

        stages {
            stage('Delete IPA') {
                steps {
                    script {
                        printStageStart("Delete IPA")
                        println "----------删除ipa文件-----------"
                        sh """
                            find ./ -type f -name "*.ipa"  |xargs rm -f
                        """
                    }
                }
            }

            stage('Analyze Channel') {
                steps {
                    script {
                        printStageStart("Analyze Channel")
                        if(env.WebhookBuildBranch) {
                            println "----------webhook式触发-----------"
                            branchName = env.WebhookBuildBranch - "refs/heads/"
                            channelMsg = "webhook"
                            xcodeConfiguration = "QA"
                            ossStoreFile = "Webhook/${xcodeConfiguration}/${branchName}"
                            println "webhook触发的分支是: " + "${branchName}"

                        } else {
                            println "-----------手动方式触发------------"
                            branchName = env.BRANCH_NAME
                            channelMsg = "手动"
                            xcodeConfiguration = params.XcodeConfiguration
                            ossStoreFile = "Manual/${xcodeConfiguration}/${branchName}"
                            println "多分支流水线分支: " + "${branchName}"
                        }

                        // 确定OSS上传和下载的路径
                        if (xcodeConfiguration == enterpriseName) {
                            if (params.Certificate == null || params.Certificate.isEmpty()) {
                                error "未在enterpriseCertificateList中配置企业签的证书集合"
                            }
                            downloadOSSIPAMainPath = "${downloadOSSImageMainPath}/${ossStoreFile}/${params.Certificate}"
                            uploadOSSPath = "${uploadOSSMainPath}/${ossStoreFile}/${params.Certificate}/" 
                        } else {
                            downloadOSSIPAMainPath = "${downloadOSSImageMainPath}/${ossStoreFile}"
                            uploadOSSPath = "${uploadOSSMainPath}/${ossStoreFile}/" 
                        }

                        needQRCode = configurationMap[xcodeConfiguration][2]
                        if(needQRCode) {
                            qrCodeInfo = "itms-services://?action=download-manifest&url=${downloadOSSIPAMainPath}/${manifestName}"
                        }
                    }
                }
            }

            stage('Modify Setting') {
                steps {
                    script {
                        printStageStart("Modify Setting")
                       // 查询.xcodeproj结尾的文件如：MyApp.xcodeproj
                        String xcodeprojName = sh(script:"""ls ./|egrep *.xcodeproj | head -1""", returnStdout: true).trim()
                        // 获取ipa包名
                        defaultIpaName = xcodeprojName.minus(".xcodeproj") + ".ipa"
                        // project.pbxproj的路径
                        pbxprojPath = "${env.WORKSPACE}/${xcodeprojName}/project.pbxproj"
                        // 寻找版本号和Build号在project.pbxproj里的id，方便读取或修改
                        String rootObjectId = sh(script:"""/usr/libexec/PlistBuddy -c "print rootObject" ${pbxprojPath}""", returnStdout: true).trim()
                        String targetId = sh(script:"""/usr/libexec/PlistBuddy -c "print objects:${rootObjectId}:targets:0" ${pbxprojPath}""", returnStdout: true).trim()
                        String buildConfigurationListId = sh(script:"""/usr/libexec/PlistBuddy -c "print objects:${targetId}:buildConfigurationList" ${pbxprojPath}""", returnStdout: true).trim()
                        // String buildConfigurationDebuge = sh(script:"""/usr/libexec/PlistBuddy -c "print objects:${buildConfigurationListId}:buildConfigurations:0" ${pbxprojPath}""", returnStdout: true).trim()
                        buildConfigurationId = findBuildConfigurationId(xcodeConfiguration, buildConfigurationListId, pbxprojPath)
                        currentVersion = sh(script:"""/usr/libexec/PlistBuddy -c "print objects:${buildConfigurationId}:buildSettings:MARKETING_VERSION" ${pbxprojPath}""", returnStdout: true).trim()
                        currentBuildNum = sh(script:"""/usr/libexec/PlistBuddy -c "print objects:${buildConfigurationId}:buildSettings:CURRENT_PROJECT_VERSION" ${pbxprojPath}""", returnStdout: true).trim()
                        println "修改前版本号：${currentVersion}"
                        println "修改前Build号：${currentBuildNum}"
                        if(params.ExpectVersion) {
                            sh """/usr/libexec/PlistBuddy -c "Set objects:${buildConfigurationId}:buildSettings:MARKETING_VERSION ${params.ExpectVersion}" ${pbxprojPath}"""
                            currentVersion = "${params.ExpectVersion}"
                            println "修改版本号成功，版本号为${currentVersion}"
                        }

                        if(params.ExpectBuild) {
                            sh """/usr/libexec/PlistBuddy -c "Set objects:${buildConfigurationId}:buildSettings:CURRENT_PROJECT_VERSION ${params.ExpectBuild}" ${pbxprojPath}"""
                            currentBuildNum = "${params.ExpectBuild}"
                            println "修改Build号成功，Build号为${currentBuildNum}"
                        }
                    }
                }
            }

            stage('Archive') {
                steps {
                    script {
                        printStageStart("Archive")
                        String webhookUrl = getWebhookUrl(notificationMap, "${params.Notification}")
                        // 修改testflight为tf，因为fastlane的testflight已被占用
                        String command = configurationMap[xcodeConfiguration][1]
                        if (command.isEmpty()) {
                            error "缺少构建命令，请在配置文件中添加构建命令"
                        }
                        
                        // 是否将build号修改为构建时间
                        Boolean modifyBuildNumToTime = configurationMap[xcodeConfiguration][3]
                        ipaName = getIPAName(currentVersion, currentBuildNum, xcodeConfiguration, enterpriseName, params.Certificate, "${map.appName}", modifyBuildNumToTime)
                        ipaDownloadUrl = "${downloadOSSIPAMainPath}/${ipaName}"
                        if (needQRCode) {
                            sh """${command} app_url:${ipaDownloadUrl} display_image_url:${map.dispalyImageUrl} full_size_image_url:${map.fullSizeImageUrl}"""
                        } else {
                            sh """${command}"""
                        }

                        if (modifyBuildNumToTime) {
                            currentBuildNum = sh(script:"""/usr/libexec/PlistBuddy -c "print objects:${buildConfigurationId}:buildSettings:CURRENT_PROJECT_VERSION" ${pbxprojPath}""", returnStdout: true).trim()
                            println "已将Build号修改为构建时间：${currentBuildNum}"
                        }
                    }
                }
            }

            stage('Upload') {
                steps {
                    script {
                        printStageStart("Upload")
                        fileOperations([
                            // 重新命名IPA名称
                            fileRenameOperation(source:defaultIpaName, destination:ipaName),
                        ])

                        if (needQRCode) {
                            sh "qrencode -o ${qrCodeImageName} \'${qrCodeInfo}\'"
                        }

                        try {
                            // 删除路径
                            sh """${ossutilmac} rm ${uploadOSSPath} -r -f"""
                        }
                        catch(Exception e) {}
                        
                        String webhookUrl = getWebhookUrl(notificationMap, "${params.Notification}")
                        // 上传ipa包至目录
                        sh """${ossutilmac} cp ${env.WORKSPACE}/${ipaName} ${uploadOSSPath} -f"""
                        if(needQRCode) {
                            // 上传manifest至目录
                            sh """${ossutilmac} cp ${env.WORKSPACE}/${manifestName} ${uploadOSSPath} -f"""
                            // 上传二维码至目录
                            sh """${ossutilmac} cp ${env.WORKSPACE}/${qrCodeImageName} ${uploadOSSPath} -f"""
                        }
                    }
                }
            }
        }

        post {
            success {
                script {
                    printStageStart("Notification")
                    // 能扫码下载APP的二维码链接
                    String qrcodeUrl = "${downloadOSSIPAMainPath}/${qrCodeImageName}"

                    MessageCard messageCard = new MessageCard();
                    messageCard.buildChannel = channelMsg;
                    messageCard.currentVersion = currentVersion;
                    messageCard.buildNum = currentBuildNum;
                    messageCard.ipaUrl = ipaDownloadUrl;
                    messageCard.qrCodeUrl = qrcodeUrl;
                    messageCard.currentBranch = branchName;
                    messageCard.xcodeConfiguration = xcodeConfiguration;
                    messageCard.needQRCode = needQRCode;
                    messageCard.installUrl = qrCodeInfo;

                    String body = getMicrosoftTeamsBody(messageCard, "${map.appName}")
                    println "----------Microsoft Body-----------"
                    println body
                    String webhookUrl = getWebhookUrl(notificationMap, "${params.Notification}")
                    sendNotificationToMicrosoftTeams(webhookUrl, body)

                    // 清空Archives
                    sh """rm -rf ~/Library/Developer/Xcode/Archives/*"""
                }
            }

            failure {
                script {
                    String body = getMicrosoftTeamsFailureBody()
                    String webhookUrl = getWebhookUrl(notificationMap, "${params.Notification}")
                    sendNotificationToMicrosoftTeams(webhookUrl, body)
                }
            }
        }
    }
}