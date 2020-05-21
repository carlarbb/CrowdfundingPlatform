$(document).ready(function() {
    var readURL = function(input) {
        if(input.files && input.files[0]) {
            var reader = new FileReader();

            reader.onload = function (e) {
                $('.avatar').attr('src', e.target.result);
            }
            reader.readAsDataURL(input.files[0]);
        }
    }

    var readURLcover = function(input){
        if (input.files && input.files[0]) {
            var reader = new FileReader();

            reader.onload = function (e) {
                $('.cover').attr('src', e.target.result);
                $('.cover').css({'visibility':'visible'});
                $("#coverPicture").val($('.cover').attr('src'));
            }
            reader.readAsDataURL(input.files[0]);
        }
    }

    $(".file-upload").on('change', function(){
        readURL(this);
    });

    $(".file-upload-cover").on('change', function(){
        readURLcover(this);
    })

    //pt pagina startCampaign -> afisare poze selectate in interfata

    $('#imagesCamp').on('change', function(){
        var total_files = document.getElementById("imagesCamp").files.length;
        $('#image_preview').html("");
        for(var i=0;i<total_files;i++)
        {
            $('#image_preview').append("<div style='position:relative;'><img class='img-responsive' " +
                "style='width:300px; height:300px;  box-shadow: 0px 2px 6px 2px rgba(0,0,0,0.75);margin:20px;'" +
                " src='"+URL.createObjectURL(event.target.files[i])+ "'></div>");
        }
    });

    $('#submitCamp').click(function() {
       $('#startCampaign').submit();
    });

    //pt pagina editCampaign -> afisare poze selectate in interfata
    $('#imagesEditCamp').on('change', function () {
        $('#editPageImgPreview').html("");
        var total_files = document.getElementById("imagesEditCamp").files.length;
        for(var i=0;i<total_files;i++)
        {
            $('#editPageImgPreview').append("<div style='position:relative;'><img class='img-responsive' " +
                "style='width:300px; height:300px;  box-shadow: 0px 2px 6px 2px rgba(0,0,0,0.75);margin:20px;'" +
                " src='"+URL.createObjectURL(event.target.files[i])+ "'></div>");
        }
    })

    $('#videoEditCamp').on('change', function () {
        $('#editPageVideoPreview').html("");

        $('#editPageVideoPreview').append("<video controls preload='none' class='video'> <source src='"+URL.createObjectURL(event.target.files[0])+ "' " +
            "type='video/mp4'></source> </video> <div class='playpause'></div>");
    });


    $('.deleteImg').click(function () {
        //eliminarea unei singure imagini din colectia de imagini ale campaniei nou-create (inputul de tip file multiple "imagesCamp")
        alert("daaa");
        //preluam imaginea curenta
       /* var currentImg = $(this).parent() > img;
        alert(currentImg.src);

        var total_images = document.getElementsById("imagesCamp").files;
        for(var image in total_images){

        }
        */
    });

    /*Pentru pagina campaignDetails*/
    /*
    $('.portfolio-item').isotope({
        itemSelector: '.item',
        layoutMode: 'fitRows'
    });

    $(document).ready(function() {
        var popup_btn = $('.popup-btn');
        popup_btn.magnificPopup({
            type : 'image',
            gallery : {
                enabled : true
            }
        });
    });*/

    //Pentru search -> la click pe icon(link) sa se transmita in href si valoarea inputului de tip text
    $('#searchIcon').click(function() {
        $('#searchForm').submit();
    });

    //Pentru modal de donate -> submit de form
    $('#donateSubmit').click(function() {
        $('#donateForm').submit();
    });

    //Pentru modal de creare categorie
    $('#addCategorySubmit').click(function() {
        $('#addCategoryForm').submit();
    });

    //Pentru modal de editare categorie
    $('#editCategorySubmit').click(function() {
        $('#editCategoryForm').submit();
    });

    //pt modal editCategory -> afisare poze selectate in interfata
    $('#imageEditCategory').on('change', function () {
        $('#editCategoryImgPreview').html("");
        var total_files = document.getElementById("imageEditCategory").files.length;

        $('#editCategoryImgPreview').append("<div style='position:relative;'><img class='img-responsive' " +
            "style='width:300px; height:300px;  box-shadow: 0px 2px 6px 2px rgba(0,0,0,0.75);margin:20px;'" +
            " src='"+URL.createObjectURL(event.target.files[0])+ "'></div>");

    });

    //Pentru modal de stergere a unei campanii fara fonduri -> submit de form
    $('#deleteCampaignSubmit').click(function() {
        $('#deleteCampaignForm').submit();
    });

    //Pentru modal de inchidere a unei campanii -> submit de form
    $('#closeCampaignSubmit').click(function() {
        $('#closeCampaignForm').submit();
    });

    //Pentru modal de stergere a unei categorii fara campanii -> submit de form
    $('#deleteCategorySubmit').click(function() {
        $('#deleteCategoryForm').submit();
    });

    //Pentru formularul de startCampaign -> navigare intre tab-uri
    $('.btnNext').click(function(){
       var t = $('.nav-tabs > .active').next('li');
      // $('.nav-tabs > .active').attr('class');
        $('.nav-tabs > .active').removeClass("active");
       t.addClass("active");
       t.find('a').trigger('click');
    });

    $('.btnPrevious').click(function(){
        var t = $('.nav-tabs > .active').prev('li');
        $('.nav-tabs > .active').removeClass("active");
        t.addClass("active");
        t.find('a').trigger('click');
    });
    //la click manual pe fiecare link din startCampaign, se muta clasa active pe li-ul corespunzator
    //(aparea problema pt ca li-ul cu Starting fields era active by default si la click pe alt link existau 2 active in acelasi timp)
    $('.nav-tabs a').click(function () {
        $('.nav-tabs > .active').removeClass("active");
        var clicked = $(this);
        clicked.parent().addClass("active");
    })

    //Pt galeria din campaignDetails

    $(".fancybox").fancybox({
        openEffect: "none",
        closeEffect: "none"
    });

    $(".zoom").hover(function(){

        $(this).addClass('transition');
    }, function(){

        $(this).removeClass('transition');
    });

    //Buton de play-pause pe toate videoclipurile
    $('.videoWrapper').click(function () {
        if($(this).children(".video").get(0).paused){
            $(this).children(".video").get(0).play();
            $(this).children(".playpause").fadeOut();
        }else{
            $(this).children(".video").get(0).pause();
            $(this).children(".playpause").fadeIn();
        }
    });

    //Setari de validare
    $("input[name=firstName]")[0].oninvalid = function () {
        if($(this).val()) {
            this.setCustomValidity("Only letters are accepted!");
        }else{
            this.setCustomValidity("Field can't be left null!");
        }
    };

    $("input[name=firstName]")[0].oninput= function () {
        this.setCustomValidity("");
    };

    $("input[name=lastName]")[0].oninvalid = function () {
        if($(this).val()) {
            this.setCustomValidity("Only letters are accepted!");
        }else{
            this.setCustomValidity("Field can't be left null!");
        }
    };

    $("input[name=lastName]")[0].oninput= function () {
        this.setCustomValidity("");
    };

    $("input[type=email]")[0].oninvalid = function () {
        if($(this).val()){
            this.setCustomValidity('Please enter a valid email!');
        }else{
            this.setCustomValidity("Field can't be left null!");
        }
    };
    $("input[type=email]")[0].oninput= function () {
        this.setCustomValidity("");
    };

    $("input[type = password]")[0].oninvalid = function () {
        if($(this).val()){
            this.setCustomValidity('Password must contain at least 8 characters, one letter and one number!');
        }
        else{
            this.setCustomValidity("Field can't be left null!");
        }
    }
    $("input[type=password]")[0].oninput= function () {
        this.setCustomValidity("");
    };
});

