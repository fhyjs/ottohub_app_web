console.log("Loaded!");
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
                  ohapp.toast("请稍后,完成后自动添加图片");
                  fetch('https://img.scdn.io/api/v1.php', {
                    method: 'POST',
                    body: formData
                  })
                  .then(response => response.json())
                  .then(function(data){
                    if(data.success===false) {
                        alert('服务器怎么死了:', data.message);
                        return;
                    }
                    $("#img_text").val(data.url);
                    $("#img_btn").click();

                  })
                  .catch(error => alert('服务器怎么死了:', error));
                };

                input.click(); // 自动弹出选择文件对话框

            });
        }
    }
    ohapp.setHomeEnable(!(window.location.href.endsWith("ottohub.cn/")||window.location.href.endsWith("ottohub.cn")));
});