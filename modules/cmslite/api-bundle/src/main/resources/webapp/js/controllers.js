(function () {

    'use strict';

    /* Controllers */

    var widgetModule = angular.module('motech-cmslite');

    widgetModule.controller('ResourceCtrl', function ($scope, Resources) {
        $scope.select = {};
        $scope.mode = 'read';
        $scope.resourceType = 'string';
        $scope.language;
        $scope.languages = ['Niemiecki', 'Polski', 'Czeski', 'Angielski'];
        $scope.filterLanguage = [];
        $scope.filterActive = false;
        $scope.usedLanguages = Resources.allLanguages();

        $scope.changeResourceType = function (type) {
            $scope.resourceType = type;
        };

        $scope.changeMode = function(mode) {
            $scope.mode = mode;
        };

        $scope.showNewResourceModal = function () {
            $scope.resourceType = 'string';
            $scope.mode = 'read';
            $scope.select = {};

            $('#newResourceModal').modal('show');
        };

        $scope.showResource = function(type, language, name) {
            switch (type) {
            case 'string':
                $scope.select = Resources.get({ type: type, language: language, name: name}, function () {
                    $('#stringResourceModal').modal('show');
                });
                break;
            case 'stream':
                $scope.select = Resources.get({ type: type, language: language, name: name}, function () {
                    $('#streamResourceModal').modal('show');
                });
                break;
            }
        };

        $scope.editStringResource = function() {
            if ($scope.validateField('stringResourceForm', 'value')) {
                blockUI();

                $('#stringResourceForm').ajaxSubmit({
                    success: function () {
                        $scope.select = Resources.get({ type: 'string', language: $scope.select.language, name: $scope.select.name}, function () {
                            $scope.changeMode('read');
                            unblockUI();
                        });
                    },
                    error: function (response) {
                        handleWithStackTrace('header.error', 'error.resource.save', response);
                        unblockUI();
                    }
                });
            }
        };

        $scope.editStreamResource = function() {
            if ($scope.validateField('streamResourceForm', 'contentFile')) {
                blockUI();

                $('#streamResourceForm').ajaxSubmit({
                    success: function () {
                        $scope.select = Resources.get({ type: 'stream', language: $scope.select.language, name: $scope.select.name}, function () {
                            $scope.changeMode('read');
                            unblockUI();
                        });
                    },
                    error: function (response) {
                        handleWithStackTrace('header.error', 'error.resource.save', response);
                        unblockUI();
                    }
                });
            }
        };

        $scope.removeResource = function(type, resource) {
            jConfirm(jQuery.i18n.prop('header.confirm.remove'), jQuery.i18n.prop("header.confirm"), function (val) {
                if (val) {
                    $scope.select.$remove({ type: type, language: resource.language, name: resource.name}, function () {
                        $scope.select = {};
                        $('#resourceTable').trigger('reloadGrid');
                        $('#' + type + 'ResourceModal').modal('hide');
                    }, alertHandler('error.removed', 'header.error'));
                }
            });
        };

        $scope.saveNewResource = function () {
            if ($scope.validateForm('newResourceForm')) {
                blockUI();
                $('#newResourceForm').ajaxSubmit({
                    success: function () {
                        $('#resourceTable').trigger('reloadGrid');
                        $('#newResourceModal').modal('hide');
                        unblockUI();
                    },
                    error: function (response) {
                        handleWithStackTrace('header.error', 'error.resource.save', response);
                        unblockUI();
                    }
                });
            }
        };

        $scope.validateForm = function (formId) {
            var name = $scope.validateField(formId, 'name'),
                language = $scope.validateField(formId, 'language'),
                value = $scope.validateField(formId, 'value'),
                contentFile = $scope.validateField(formId, 'contentFile');

            return name && language && ($scope.resourceType === 'string' ? value : contentFile);
        };

        $scope.validateField = function (formId, key) {
            var field = $('#' + formId + ' #' + key),
                hint = $('#' + formId + ' #' + key).next('span'),
                validate = field.val() !== undefined && field.val() !== '';

            if (validate) {
                hint.hide();
            } else {
                hint.show();
            }

            return validate;
        };

        $scope.clickFilterLanguages = function () {
            if ($scope.filterActive !== true) {
                $('#collapse-resource').addClass('viewPopover');
                $('#collapse-resource').removeClass('collapse');

            } else {
                $('#collapse-resource').addClass('collapse');
                $('#collapse-resource').removeClass('viewPopover');
            }
            $scope.filterActive = !($scope.filterActive);
        };

        $scope.removeLanguage = function (selectedLanguage) {
            var indexArray = $scope.filterLanguage.indexOf(selectedLanguage);
            if (indexArray !== -1) {
                $scope.filterLanguage.splice(indexArray,1);
            };
        };

        $scope.selectLanguages = function (selectedLanguage) {
            if ($scope.filterLanguage.indexOf(selectedLanguage) !== -1) {
                $scope.filterLanguage.forEach($scope.removeLanguage(selectedLanguage))
            } else {
                $scope.filterLanguage.push(selectedLanguage);
            }
        };
    });

}());
