<cm_document_form>
    <div class="panel panel-default">
        <div class="panel-heading" if={ self.mode == 'create' }><h2>{ labels.document_new[app.language] } [{ doc.status }]</h2></div>
        <div class="panel-heading" if={ self.mode == 'update' }><h2>{ labels.document_modify[app.language] } [{ doc.status }]</h2></div>
        <div class="panel-heading" if={ self.mode == 'view' }><h2>{ labels.document_view[app.language] } [{ doc.status }]</h2></div>
        <div class="panel-body">
            <form onsubmit={ self.submitForm }>
                <div class="form-group">
                    <label for="type">{ labels.type[app.language] }</label>
                    <select class="form-control" id="type" name="type" value={ doc.type } onchange={ self.changeType } readonly={ self.mode != 'create'  } required>
                        <option value="ARTICLE">ARTICLE</option>
                        <option value="CODE">CODE</option>
                        <option value="FILE">FILE</option>
                    </select>
                </div>
                <div class="form-group">
                    <label for="name">{ labels.name[app.language] }</label>
                    <input class="form-control" id="name" name="name" type="text" value={ doc.name } readonly={ self.mode != 'create'  } required>
                </div>
                <div class="form-group">
                    <label for="path">{ labels.path[app.language] }</label>
                    <input class="form-control" id="path" name="path" type="text" value={ doc.path } readonly={ self.mode != 'create'  } >
                </div>
                <div class="form-group">
                    <label for="uid">{ labels.uid[app.language] }</label>
                    <input class="form-control" id="uid" name="uid" type="text" value={ doc.uid } readonly={ true }>
                </div>
                <div class="form-group">
                    <label for="author">{ labels.author[app.language] }</label>
                    <input class="form-control" id="author" name="author" type="text"  value={ doc.author } readonly={ !allowEdit }>
                </div>
                <div class="form-group">
                    <label for="title">{ labels.title[app.language] }</label>
                    <input class="form-control" id="title" name="title" type="text" value={ doc.title } readonly={ !allowEdit }>
                </div>
                <div class="form-group">
                    <label for="summary">{ labels.summary[app.language] }</label>
                    <textarea class="form-control" rows="3" id="summary" name="summary" readonly={ !allowEdit }>{ doc.summary }</textarea>
                </div>
                <div class="form-group" if={ doc.type != 'FILE' }>
                    <label for="mimeType">{ labels.mimeType[app.language] }</label>
                    <input class="form-control" id="mimeType" name="mimeType" type="text" value={ doc.mimeType }>
                </div>
                <div class="form-group" if={ doc.type == 'FILE' }>
                    <label for="mimeType">{ labels.mimeType[app.language] }</label>
                    <input class="form-control" id="mimeType" name="mimeType" type="text" value={ doc.mimeType } readonly>
                </div>
                <div class="form-group" if={ doc.type != 'FILE' }>
                    <label for="content">{ labels.content[app.language] }</label>
                    <textarea class="form-control" style="white-space:pre-wrap"
                              rows="10" id="content" name="content" readonly={ !allowEdit }>{ doc.content }</textarea>
                </div>
                <div class="form-group" if={doc.type == 'FILE'}>
                    <label for="content">{ labels.content[app.language] }</label>
                    <input class="form-control" id="content" name="content" value={ doc.content } readonly>
                </div>
                <div class="form-group">
                    <label for="tags">{ labels.tags[app.language] }</label>
                    <input class="form-control" id="tags" name="tags" type="text" value={ doc.tags } readonly={ !allowEdit }>
                </div>
                <div class="form-group">
                    <label for="language">{ labels.language[app.language] }</label>
                    <input class="form-control" id="language" name="language" type="text" value={ doc.language } readonly={ true }>
                </div>
                <div class="form-group">
                    <label for="commentable">{ labels.commentable[app.language] }</label>
                    <input class="form-control" id="commentable" name="commentable" type="text" value={ doc.commentable } readonly={ !allowEdit }>
                </div>
                <div class="form-group" if={doc.type == 'FILE' && allowEdit}>
                    <label for="file">{ labels.selectFile[app.language]}</label><br />
                    <input class="form-control-file" type="file" name="file" id="file">
                </div>
                <button type="button" onclick={ close } class="btn btn-default pull-right">{ labels.cancel[app.language] }</button>
                <span>&nbsp;</span>
                <button type="submit" class="btn btn-default" disabled={ !allowEdit }>{ labels.save[app.language] }</button>
            </form>
<!--
            <form class="container" id="form1" enctype="multipart/form-data" method="post" action="Upload.aspx" onsubmit={ self.uploadFile }>
                <div class="row setup-content" id="step-1">
                    <div class="col-xs-12">
                        <div class="col-md-12 well text-center">
                            <label for="fileToUpload">Select a File to Upload</label><br />
                            <input type="file" name="fileToUpload" id="fileToUpload" onchange={self.fileSelected}/>
                        </div>
                        <div id="fileName"></div>
                        <div id="fileSize"></div>
                        <div id="fileType"></div>
                        <div class="row">
                            <input type="submit" value="Upload" />
                        </div>
                        <div id="progressNumber"></div>

                    </div>
                </div>
            </form>
-->
        </div>
</div>
<script>
    this.visible = true
    self = this
    self.listener = riot.observable()
    self.callbackListener
    self.allowEdit = false
    self.method = 'POST'
    self.mode = 'view'
    self.selectedLanguage = app.language
    self.selectedStatus = 'wip'
    self.doc = {
        'uid': '',
        'author': '',
        'name': '',
        'path': '',
        'title': '',
        'summary': '',
        'content': '',
        'tags': '',
        'language': '',
        'type': 'ARTICLE',
        'mimeType': '',
        'status': '',
        'created': '',
        'modified': '',
        'published': '',
        'createdBy': '',
        'commentable': false,
        'size': 0
    }

    globalEvents.on('data:submitted', function (event) {
        if (app.debug) {
            console.log("I'm happy!")
        }
    });

    init(eventListener, uid, editable, language, status, path){
        self.callbackListener = eventListener
        self.allowEdit = editable
        self.selectedLanguage = language
        self.selectedStatus = status
        self.doc.language = language
        self.doc.path = path
        self.doc.status = status
        self.doc.mimeType = 'text/html'
        self.method = 'POST'
        app.log('HELLO ' + uid)
        app.log('CALLBACK: ' + self.callbackListener)
        app.log('EDITABLE: ' + self.allowEdit)
        if (uid != 'NEW') {
            readDocument(uid)
            self.method = 'PUT'
            if (self.allowEdit) {
                self.mode = 'update'
            } else {
                self.mode = 'view'
            }
        } else {
            self.mode = 'create'
            self.selectedStatus = 'wip'
            self.doc.status = 'wip'
        }

    }
    
/*
    self.fileSelected = function(e) {
        e.preventDefault()
        var file = document.getElementById('fileToUpload').files[0];
        if (file) {
          var fileSize = 0;
          if (file.size > 1024 * 1024)
            fileSize = (Math.round(file.size * 100 / (1024 * 1024)) / 100).toString() + 'MB';
          else
            fileSize = (Math.round(file.size * 100 / 1024) / 100).toString() + 'KB';

          //document.getElementById('fileName').innerHTML = 'Name: ' + file.name;
          //document.getElementById('fileSize').innerHTML = 'Size: ' + fileSize;
          //document.getElementById('fileType').innerHTML = 'Type: ' + file.type;
        }
      }
*/
    self.changeType = function(e){
        e.preventDefault()
        self.doc.type = e.target.value
        riot.update()
    }
    
    self.submitForm = function (e) {
        e.preventDefault()

        docPath = ''
        if (e.target.elements['uid']) {
            docPath = (self.method == 'PUT') ? e.target.elements['uid'].value : ''
        }

        var fd = new FormData(e.target)
        var pth = ''
        if(fd.get('path')){
            pth=fd.get('path').trim()
        }
        var name=fd.get('name').trim()
        if (!pth.endsWith('/')) {
            pth = pth + '/'
        }
        fd.set('path',pth)
        while(name.startsWith('/')){
            name=name.substring(1)
        }
        while(name.endsWith('/')){
            name=name.substring(0,name.length-1)
        }
        fd.set('name', name)
        fd.set('uid',pth+name)
        //var c=fd.get('content').replace(/\r\n|\r|\n/g,"&#10;")
        var c=escape(fd.get('content'))
        fd.set('content',c)
        console.log(c)
        sendFormData(fd, self.method, app.cmAPI + docPath, app.user.token, self.close, globalEvents, 'submit:OK', 'submit:ERROR', app.debug, null)
    }

    self.close = function (object) {
        var text = '' + object
        console.log('CALBACK: ' + object)
        if (text.startsWith('{')) {
            var tmpDoc = self.doc = JSON.parse(text)
            self.callbackListener.trigger('submitted:'+tmpDoc.path)
        } else if (text.startsWith('error')) {
            alert(text)
        } else if (text.startsWith('[object MouseEvent')) {
            self.callbackListener.trigger('cancelled')
        }
    }

    var update = function (text) {
        app.log("DOC: " + text)
        self.doc = JSON.parse(text);
        //console.log(self.doc.content)
        //self.doc.content = atou(self.doc.content)
        //console.log(self.doc.content)
        self.doc.content=unescape(self.doc.content)
        riot.update();
    }

    var readDocument = function (docUid) {
        getData(app.cmAPI + docUid + '?language=' + self.selectedLanguage + '&status=' + self.selectedStatus,
                null,
                app.user.token,
                update,
                self.listener, //globalEvents
                'OK',
                null, // in case of error send response code
                app.debug,
                globalEvents
                );
    }


    this.labels = {
        "document_view": {
            "en": "Document Preview",
            "pl": "Podgląd Dokumentu"
        },
        "document_new": {
            "en": "New Document",
            "pl": "Nowy Dokument"
        },
        "document_modify": {
            "en": "Modify Dokument",
            "pl": "Edycja Dokumentu"
        },
        "name": {
            "en": "* Name",
            "pl": "* Nazwa"
        },
        "uid": {
            "en": "UID",
            "pl": "UID"
        },
        "path": {
            "en": "Path",
            "pl": "Ścieżka"
        },
        "title": {
            "en": "Title",
            "pl": "Tytuł"
        },
        "author": {
            "en": "Author",
            "pl": "Autor"
        },
        "summary": {
            "en": "Summary",
            "pl": "Streszczenie"
        },
        "content": {
            "en": "Content",
            "pl": "Treść"
        },
        "tags": {
            "en": "Tags",
            "pl": "Znaczniki"
        },
        "language": {
            "en": "Language",
            "pl": "Język"
        },
        "type": {
            "en": "* Type",
            "pl": "* Typ"
        },
        "mimeType": {
            "en": "Mime type",
            "pl": "Mime type"
        },
        "commentable": {
            "en": "Can be commented?",
            "pl": "Można komentować?"
        },
        "selectFile": {
            "en": "Select a File to Upload",
            "pl": "Wybierz plik do przesłania"
        },
        "save": {
            "en": "Save",
            "pl": "Zapisz"
        },
        "cancel": {
            "en": "Cancel",
            "pl": "Porzuć"
        }
    }
</script>
</cm_document_form>