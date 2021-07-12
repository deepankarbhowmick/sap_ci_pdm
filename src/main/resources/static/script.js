function getPost(url, request, target, spinner) {
  fetch(url, request)
    .then((response) => {
      //Hide the spinner element.
      document.getElementById(spinner).setAttribute('hidden', '');
      if (!response.ok) {
        //Response body will contain the error message.
        response.text().then((text) => alert(text));
      } else {
        //Render result in target.
        response.text().then((text) => (document.getElementById(target).innerHTML = text));
      }
    })
    .catch((error) => {
      //Hide the spinner element.
      document.getElementById(spinner).setAttribute('hidden', '');
      alert(error); //Network error.
    });
}

function getData(url, element = [], elementId) {
  let item, urlFinal;
  if (element.length > 0) {
    url = url + '?';
    for (eachElement of element) {
      switch (document.getElementById(eachElement).type.toLowerCase()) {
        case 'textarea':
        case 'select-one':
        case 'hidden':
        case 'text':
          item = document.getElementById(eachElement).value;
          break;
        case 'radio':
          item = document.querySelector(`input[name=${eachElement.name}]:checked`).value;
          break;
      }
      url = url + `${eachElement}=${item}&`;
    }
    urlFinal = url.substring(url, url.length - 1);
  } else {
    urlFinal = url;
  }
  removeAll('table', 'spinner');
  getPost(urlFinal, {'method' : 'GET'}, 'table', 'spinner');
}

function postData(url, e, entity, elementId, v) {
  let formName = e.getAttribute('data-form_id');
  let request = {}, request_ = {};
  for (element of document.forms[formName]) {
    switch (element.type.toLowerCase()) {
      case 'textarea':
      case 'select-one':
      case 'hidden':
      case 'text':
        request[element.name] = element.value;
        break;
      case 'radio':
        request[element.name] = document.querySelector(`input[name=${element.name}]:checked`).value;
        break;
    }
  }
  request['entity'] = document.getElementById(entity).value;
  request_ = { 'method' : 'POST',
               'headers' : { 'Content-Type': 'application/json' },
               'body': JSON.stringify(request) }
  removeAll('table', 'spinner');
  getPost(url, request_, 'table', 'spinner');
  v.preventDefault(); //To prevent the default behavior of page refresh after form-POST.
}

function removeAll(target, spinner) {
  let table = document.getElementById(target);
  while (table.firstChild) {
    table.removeChild(table.lastChild);
  }
  document.getElementById(spinner).removeAttribute('hidden');
}