import os
from flask import Flask, request, jsonify, send_file
from flask import Flask, jsonify
from flask_socketio import SocketIO
from watchdog.observers import Observer
import requests
from watchdog.events import FileSystemEventHandler
from queue import Queue
from io import BytesIO
import threading
from pymongo import MongoClient
import yagmail
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from email.mime.image import MIMEImage
from tensorflow import keras
from keras.models import load_model
import cv2
import numpy as np

model = load_model("Model4.h5")
labels_dictionary = {'Chin-su': 0, 'Coca': 1,
                     'Tea-plus': 2, 'Cafe': 3, '7-up': 4, 'Snack': 5, 'My Tom': 6, 'Gau Bong': 7, 'Sac Dien Thoai': 8, 'Bo Bai': 9}


app = Flask(__name__)
UPLOAD_FOLDER = 'uploads'
socketio = SocketIO(app)
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER


event_queue = Queue()


client = MongoClient("mongodb://localhost:27017/")


db = client.PBL5
collection = db.Product
users = db.Account


class UploadsEventHandler(FileSystemEventHandler):
    def on_created(self, event):
        if event.is_directory:
            return
        if event.src_path.endswith('.jpg'):
            print(f"New image detected: {event.src_path}")
            event_queue.put(event)


def start_observer(path):
    event_handler = UploadsEventHandler()
    observer = Observer()
    observer.schedule(event_handler, path, recursive=False)
    observer.start()
    try:
        while True:
            observer.join(1)
    except KeyboardInterrupt:
        observer.stop()
    observer.join()


@app.route('/login', methods=['POST'])
def login():

    user = request.json['username']
    password = request.json['password']

    user = users.find_one({"username": user})

    if user["password"] == password:
        return jsonify({"message": "Login successful"}), 200
    else:
        return jsonify({"error": "Invalid email or password"}), 401


@app.route('/signup', methods=['POST'])
def register():

    email = request.json['email']
    password = request.json['password']
    username = request.json['username']

    if users.find_one({"email": email}):
        return jsonify({"error": "Email already exists"}), 400

    users.insert_one(
        {"email": email, "password": password, "username": username})
    return jsonify({"message": "User registered successfully"}), 201


@app.route('/thanhToan', defaults={'email': None}, methods=['PATCH'])
@app.route('/thanhToan/<string:email>', methods=['PATCH'])
def checkout(email):

    item_names = request.json

    if not item_names or not isinstance(item_names, list):
        print("errrrrrrr")
        return jsonify({'message': 'itemName list is required and should be a list'}), 400

    print("Received item names:", item_names)

    productsSendToEmail = []

    for name in item_names:
        print("Product name:", name)

        product = collection.find_one({'name': name})
        productsSendToEmail.append({
            'name': product.get('name'),
            'orderedQuantity': 0,
            'price': product.get('price')
        })
        response_items = []
        if product:
            print(f"Product found: {product}")
            current_quantity = product.get('quantity', 0)

            if current_quantity > 0:
                new_quantity = current_quantity - 1
                collection.update_one(
                    {'name': name}, {'$set': {'quantity': new_quantity}})
                productsSendToEmail[-1]['orderedQuantity'] += 1
                response_items.append(
                    {'name': name, 'old_quantity': current_quantity, 'new_quantity': new_quantity})
                print(f"Updated quantity for {name}: {new_quantity}")
            else:
                response_items.append({'name': name, 'error': 'Out of stock'})
                print(f"Cannot update quantity for {name}: Out of stock")

        else:
            response_items.append({'name': name, 'error': 'Product not found'})
            print(f"Product {name} not found in the database.")

    print("orderedList :" + str(productsSendToEmail))

    total_cost = sum(product['orderedQuantity'] * product['price']
                     for product in productsSendToEmail)
    html_content = """
    <html>
    <head>
        <style>
            table {
                width: 100%;
                border-collapse: collapse;
            }
            table, th, td {
                border: 1px solid black;
            }
            th, td {
                padding: 10px;
                text-align: center;
            }
            th {
                background-color: #f2f2f2;
            }
            h2 {
                margin-left: 30px;
                text-align:center;
            }
        </style>
    </head>
    <body>
        <h2>            Chi Tiết Hóa Đơn</h2>
        <table>up
            <tr>
                <th>Tên sản phẩm</th>
                <th>Số lượng</th>
                <th>Giá</th>
            </tr>
    """

    # Thêm sản phẩm vào bảng
    for product in productsSendToEmail:
        # with open(product['image_link'], 'rb') as img_file:
        #     img_data = img_file.read()
        img_cid = product['name'].replace(" ", "_").lower()

        html_content += f"""
            <tr>
                <td>{product['name']}</td>
                <td>{product['orderedQuantity']}</td>
                <td>{product['price']} VND</td>
            </tr>
        """
    # Thêm tổng hóa đơn vào bảng
    html_content += f"""
            <tr>
                <td colspan="3" style="text-align:center;"><strong>Tổng cộng:</strong></td>
                <td><strong>{total_cost} VND</strong></td>
            </tr>
        </table>
    </body>
    </html>
    """

    # Cấu hình thông tin gửi mail
    sender_email = "quythaingoc13@gmail.com"
    # receiver_email = "baobap208@gmail.com"
    subject = "Hóa đơn mua hàng"
    body = "Cảm ơn bạn đã mua hàng. Dưới đây là chi tiết hóa đơn của bạn:"

    # Khởi tạo đối tượng Yagmail
    yag = yagmail.SMTP(user=sender_email, password='shvo imea nscy vmyv')

    # Tạo email với HTML và ảnh đính kèm
    msg = MIMEMultipart()
    msg['From'] = sender_email
    # msg['To'] = receiver_email
    msg['Subject'] = subject

    msg.attach(MIMEText(body, 'plain'))
    msg.attach(MIMEText(html_content, 'html'))

    if email:
        sender_email = "quythaingoc13@gmail.com"
        subject = "Hóa đơn mua hàng"
        body = "Cảm ơn bạn đã mua hàng. Dưới đây là chi tiết hóa đơn của bạn:"

        # Khởi tạo đối tượng Yagmail
        yag = yagmail.SMTP(user=sender_email, password='shvo imea nscy vmyv')

        try:
            yag.send(to=email, subject=subject, contents=[body, html_content])
            print("Email sent successfully!")
        except Exception as e:
            print(f"Failed to send email: {e}")

    return jsonify({'message': 'Checkout processed', 'items': response_items}), 200


# @app.route('/image', methods=['GET'])
# def process_image():
#     # Xử lý sự kiện từ hàng đợi
#     while not event_queue.empty():
#         event = event_queue.get()
#         if event:
#             image_path = event.src_path
#             # Xử lý ảnh ở đây
#             data = {'message': 'Image processed successfully',
#                     'image_path': image_path}
#             return jsonify(data, send_file(image_path, mimetype='image/jpeg'))
#     # Trả về lỗi nếu không có sự kiện trong hàng đợi
#     return jsonify({'error': 'No image event in queue'}), 400

detection_result = ["demo"]


# @app.route('/image', methods=['GET'])
# def process_image():
#     # Xử lý sự kiện từ hàng đợi
#     # ItemName = detection_result[-1]
#     # product = collection.find_one({'name': ItemName})
#     while not event_queue.empty():
#         event = event_queue.get()
#         if event:
#             image_path = event.src_path
#             # Trả về tệp ảnh trực tiếp
#             product = collection.find_one({'name': 'Coca'})
#             product['_id'] = str(product['_id'])
#             # return send_file(image_path, mimetype='image/jpeg')
#             return jsonify({'product': product}), 200
#     # Trả về lỗi nếu không có sự kiện trong hàng đợi
#     return jsonify({'error': 'No image event in queue'}), 400


@app.route('/image', methods=['GET'])
def process_image():
    if not image_uploaded.is_set():
        return jsonify({'error': 'No image has been uploaded'}), 400

    while not event_queue.empty():
        event = event_queue.get()
        if event:
            print(f"New image detected: {event.src_path}")
            # Lấy đường dẫn ảnh
            image_path = event.src_path

            # Kiểm tra xem ảnh có tồn tại trong thư mục upload hay không
            if os.path.exists(image_path):
                # Đọc sản phẩm từ MongoDB dựa trên tên ảnh
                image_name = os.path.basename(image_path)
                print("name:" + detection_result[-1])
                product = collection.find_one({'name': detection_result[-1]})
                print(product)
                if product:
                    # Chuyển ObjectId thành chuỗi để phù hợp với JSON
                    product['_id'] = str(product['_id'])
                    return jsonify({'product': product}), 200
                else:
                    print(f'Product  not found in the database')
                    return jsonify({'error': f'Product "{image_name}" not found in the database'}), 404
            else:
                return jsonify({'error': f'Image "{image_path}" not found'}), 404

    # Trả về lỗi nếu không có sự kiện trong hàng đợi
    image_uploaded.clear()
    return jsonify({'error': 'No image event in queue'}), 400


image_uploaded = threading.Event()


@app.route('/upload', methods=['POST'])
def upload_image():
    global image_uploaded
    image_data = request.data

    # Tìm số thứ tự tiếp theo cho tệp hình ảnh
    next_image_number = 1
    while os.path.exists(os.path.join(app.config['UPLOAD_FOLDER'], f'image{next_image_number}.jpg')):
        next_image_number += 1

    # Tạo tên file mới với số thứ tự đã tìm được
    filename = os.path.join(
        app.config['UPLOAD_FOLDER'], f'image{next_image_number}.jpg')

    with open(filename, 'wb') as f:
        f.write(image_data)
    print(f'Received image saved as {filename}')

    # Read the uploaded image for processing
    img = cv2.imread(filename)
    img = cv2.resize(img, (128, 128))
    img = np.array(img)
    img = np.expand_dims(img, axis=0)
    img = img / 255.0
    predictions = model.predict(img)
    print(f"Predictions: {predictions}")
    predicted_class = predictions.argmax()

    for key, value in labels_dictionary.items():
        if value == predicted_class:
            detection_result.append(key)
            print(key)
    image_uploaded.set()
    return 'Image received successfully'


@app.route('/getAll', methods=['GET'])
def get_all_products():
    # Exclude the '_id' field from the result
    products = list(collection.find({}, {'_id': 0}))
    print(products)
    return jsonify({'products': products}), 200


BASE_URL = "https://api.vietqr.io/image/970422-0342280638-1kIOf9a.jpg"


@app.route('/getQR', methods=['POST'])
def generate_url():

    item_names = request.json
    account_name = request.args.get('accountName', 'THAI%20NGOC%20QUY')
    add_info = request.args.get('addInfo', 'Thanh%20toan%20hoa%20don')

    if not item_names or not isinstance(item_names, list):
        return jsonify({'message': 'itemName list is required and should be a list'}), 400

    total_amount = 0
    for name in item_names:

        product = collection.find_one({'name': name})
        if product:

            price = product.get('price', 0)
            total_amount += price

    new_url = f"{BASE_URL}?accountName={account_name}&amount={total_amount}&addInfo={add_info}"

    response = requests.get(new_url)
    if response.status_code != 200:
        return jsonify({'message': 'Failed to download QR code image'}), 500

    image_bytes = BytesIO(response.content)

    return send_file(image_bytes, mimetype='image/jpeg', as_attachment=True, download_name='qrcode.jpg')


@app.route('/create/sanPham', methods=['POST'])
def addSP():
    data = request.json
    print(data)
    if not data:
        return jsonify({'message': 'Dữ liệu đầu vào không được cung cấp'}), 400

    id = data.get('p_id')
    print(id)
    name = data.get('name')
    p_id = id
    price = data.get('price')
    # Giá trị mặc định của số lượng là 0 nếu không được cung cấp
    quantity = data.get('quantity', 0)
    image = data.get('image')
    existing_product = collection.find_one({'name': name})
    if existing_product:
        return jsonify({'message': 'Sản phẩm với tên này đã tồn tại'}), 400
    product = {
        'p_id': p_id,
        'name': name,
        'price': price,
        'quantity': quantity,

    }

    result = collection.insert_one(product)
    product['_id'] = str(result.inserted_id)
    return jsonify({'message': 'Sản phẩm đã được tạo thành công', 'product': product}), 200


@app.route('/delete/<p_id>', methods=['DELETE'])
def deleteSP(p_id):
    print(p_id)
    try:
        p_id = int(p_id)
        # Xóa sản phẩm theo p_id
        result = collection.delete_one({'p_id': p_id})

        if result.deleted_count == 0:
            return jsonify({'error': 'Product not found'}), 404

        return jsonify({'message': 'Product deleted successfully'}), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500


if __name__ == "__main__":

    if not os.path.exists(UPLOAD_FOLDER):
        os.makedirs(UPLOAD_FOLDER)

    observer_thread = threading.Thread(
        target=start_observer, args=(UPLOAD_FOLDER,))
    observer_thread.daemon = True
    observer_thread.start()

    # Start the Flask app
    app.run(host='0.0.0.0', port=8080)
