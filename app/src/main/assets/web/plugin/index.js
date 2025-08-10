console.log("Loaded!");
window.ohapp.plugins = [];

$(document).ready(function(){
    if(window.location.href.includes("ottohub.cn")){
        $("#blog_page").show();
        $("#video_page").show();
        $("#blog_page").css("overflow","visible");
        $("#video_page").css("overflow","visible");
        $("#search").hide();
        if($('#search').length <= 0){
            ohapp.setSearchEnable(false);
        }else{
            ohapp.setSearchEnable(true);
        }
        if(window.location.href.includes("ottohub.cn/submit/blog")){
            $("#toolbar").css("height","19vh");
            $("#content_text").css("height","67vh");
            var extBtn = $(`<div id="upload_btn" class="btn waves-effect waves-light light-blue lighten-3 white-text">本地上传图片</div>
            <style>
                    #upload_btn {
                       position: absolute;
                       width: 95%;
                       height: 5vh;
                       left: 3vw;
                       top: 13vh;
                       border-radius: 1vh;
                       font-size: 2.3vh;
                       cursor: pointer;
                       display: flex;
                       justify-content: center;
                       align-items: center;
                       white-space: nowrap;
                   }
            </style>
            `);
            $("#toolbar").append(extBtn);
            extBtn.click(function(){
                const input = document.createElement('input');
                input.type = 'file';
                input.accept = 'image/*'; // 只允许选择图片

                input.onchange = e => {
                  const formData = new FormData();
                  formData.append('image', e.target.files[0]);
                  formData.append('token', "1c17b11693cb5ec63859b091c5b9c1b2");
                  ohapp.toast("请稍后,完成后自动添加图片");
                  $.ajax({
                      url: 'https://hanana2.link/ottohub/EasyImages2.0/api/index.php',
                      type: 'POST',
                      data: formData,
                      contentType: false,
                      processData: false,
                      xhr: function() {
                        const xhr = new window.XMLHttpRequest();

                        // 上传进度监听
                        xhr.upload.addEventListener("progress", function(evt) {
                          if (evt.lengthComputable) {
                            const percentComplete = (evt.loaded / evt.total) * 100;
                            console.log('上传进度: ' + percentComplete.toFixed(2) + '%');
                            ohapp.setProgress(percentComplete);
                            // 你可以更新页面进度条，比如：
                            // $('#progressBar').css('width', percentComplete + '%').text(percentComplete.toFixed(2) + '%');
                          }
                        }, false);

                        return xhr;
                      },
                      success: function(data) {
                        if(data.code != 200) {
                          alert('服务器怎么死了: ' + data.result);
                          return;
                        }
                        console.log(data);
                        alert('成功: ' + data.result);
                        $("#img_text").val(data.url);
                        $("#img_btn").click();
                      },
                      error: function(xhr, status, error) {
                        alert('服务器怎么死了: ' + error);
                      }
                    });
                    }
                input.click(); // 自动弹出选择文件对话框

            });
        }
    }
    ohapp.setHomeEnable(!(window.location.href.endsWith("ottohub.cn/")||window.location.href.endsWith("ottohub.cn")));
});