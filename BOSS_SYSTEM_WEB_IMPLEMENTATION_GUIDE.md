# Boss System Web Interface Implementation Guide

## Technology Stack

### Frontend
- **React.js** với TypeScript
- **Ant Design** cho UI components
- **React Query** cho data fetching
- **React Router** cho routing
- **Chart.js** cho statistics charts

### Backend
- **Spring Boot** với Java
- **MySQL** database
- **JWT** authentication
- **WebSocket** cho real-time updates

## Project Structure

```
boss-management-web/
├── src/
│   ├── components/
│   │   ├── BossList.tsx
│   │   ├── BossForm.tsx
│   │   ├── BossDetails.tsx
│   │   ├── BossStatistics.tsx
│   │   └── BossCategories.tsx
│   ├── pages/
│   │   ├── Dashboard.tsx
│   │   ├── BossManagement.tsx
│   │   └── Settings.tsx
│   ├── services/
│   │   ├── bossApi.ts
│   │   ├── itemApi.ts
│   │   └── authApi.ts
│   ├── hooks/
│   │   ├── useBosses.ts
│   │   ├── useBossForm.ts
│   │   └── useWebSocket.ts
│   ├── types/
│   │   ├── boss.ts
│   │   ├── item.ts
│   │   └── api.ts
│   └── utils/
│       ├── validation.ts
│       ├── formatters.ts
│       └── constants.ts
```

## Key Components Implementation

### 1. Boss List Component
```typescript
import React, { useState } from 'react';
import { Table, Button, Input, Select, Space } from 'antd';
import { useBosses } from '../hooks/useBosses';

const BossList: React.FC = () => {
  const [filters, setFilters] = useState({
    search: '',
    category: '',
    isActive: undefined
  });
  
  const { data, loading, refetch } = useBosses(filters);
  
  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80
    },
    {
      title: 'Tên',
      dataIndex: 'name',
      key: 'name',
      filteredValue: filters.search ? [filters.search] : null,
      onFilter: (value: string, record: Boss) => 
        record.name.toLowerCase().includes(value.toLowerCase())
    },
    {
      title: 'Dame',
      dataIndex: 'dame',
      key: 'dame',
      render: (dame: number) => dame.toLocaleString()
    },
    {
      title: 'HP',
      dataIndex: 'hp',
      key: 'hp',
      render: (hp: number[]) => hp[0].toLocaleString()
    },
    {
      title: 'Trạng thái',
      dataIndex: 'isActive',
      key: 'isActive',
      render: (isActive: boolean) => 
        isActive ? 'Hoạt động' : 'Không hoạt động'
    },
    {
      title: 'Hành động',
      key: 'actions',
      render: (_, record: Boss) => (
        <Space>
          <Button size="small" onClick={() => viewBoss(record.id)}>
            Xem
          </Button>
          <Button size="small" onClick={() => editBoss(record.id)}>
            Sửa
          </Button>
          <Button size="small" danger onClick={() => deleteBoss(record.id)}>
            Xóa
          </Button>
        </Space>
      )
    }
  ];
  
  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <Space>
          <Input.Search
            placeholder="Tìm kiếm boss..."
            value={filters.search}
            onChange={(e) => setFilters({...filters, search: e.target.value})}
            style={{ width: 200 }}
          />
          <Select
            placeholder="Chọn category"
            value={filters.category}
            onChange={(value) => setFilters({...filters, category: value})}
            style={{ width: 150 }}
          >
            <Option value="Android Series">Android Series</Option>
            <Option value="Bill & Whis Series">Bill & Whis Series</Option>
            <Option value="Monster Series">Monster Series</Option>
          </Select>
        </Space>
      </div>
      
      <Table
        columns={columns}
        dataSource={data?.bosses}
        loading={loading}
        rowKey="id"
        pagination={{
          current: data?.pagination.page,
          total: data?.pagination.total,
          pageSize: data?.pagination.limit
        }}
      />
    </div>
  );
};
```

### 2. Boss Form Component
```typescript
import React from 'react';
import { Form, Input, InputNumber, Select, Button, Card, Row, Col } from 'antd';
import { useBossForm } from '../hooks/useBossForm';

const BossForm: React.FC<{bossId?: number}> = ({ bossId }) => {
  const { form, loading, onSubmit } = useBossForm(bossId);
  
  return (
    <Form form={form} onFinish={onSubmit} layout="vertical">
      <Row gutter={16}>
        <Col span={12}>
          <Card title="Thông tin cơ bản">
            <Form.Item
              name="name"
              label="Tên boss"
              rules={[{ required: true, message: 'Vui lòng nhập tên boss' }]}
            >
              <Input />
            </Form.Item>
            
            <Form.Item
              name="gender"
              label="Giới tính"
              rules={[{ required: true, message: 'Vui lòng chọn giới tính' }]}
            >
              <Select>
                <Option value={0}>Trái Đất</Option>
                <Option value={1}>Xayda</Option>
                <Option value={2}>Namec</Option>
              </Select>
            </Form.Item>
            
            <Form.Item
              name="dame"
              label="Sát thương"
              rules={[{ required: true, message: 'Vui lòng nhập sát thương' }]}
            >
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
          </Card>
        </Col>
        
        <Col span={12}>
          <Card title="Thông tin chi tiết">
            <Form.Item
              name="hp"
              label="Máu"
              rules={[{ required: true, message: 'Vui lòng nhập máu' }]}
            >
              <InputNumber min={1} style={{ width: '100%' }} />
            </Form.Item>
            
            <Form.Item
              name="mapJoin"
              label="Map xuất hiện"
              rules={[{ required: true, message: 'Vui lòng chọn map' }]}
            >
              <Select mode="multiple">
                <Option value={9}>Map 9</Option>
                <Option value={16}>Map 16</Option>
                <Option value={24}>Map 24</Option>
              </Select>
            </Form.Item>
            
            <Form.Item
              name="isActive"
              label="Trạng thái"
              initialValue={true}
            >
              <Select>
                <Option value={true}>Hoạt động</Option>
                <Option value={false}>Không hoạt động</Option>
              </Select>
            </Form.Item>
          </Card>
        </Col>
      </Row>
      
      <div style={{ textAlign: 'right', marginTop: 16 }}>
        <Button type="primary" htmlType="submit" loading={loading}>
          {bossId ? 'Cập nhật' : 'Tạo mới'}
        </Button>
      </div>
    </Form>
  );
};
```

### 3. Boss Statistics Dashboard
```typescript
import React from 'react';
import { Card, Row, Col, Statistic } from 'antd';
import { Pie, Bar } from 'react-chartjs-2';

const BossStatistics: React.FC = () => {
  const { data: stats } = useBossStatistics();
  
  const categoryData = {
    labels: Object.keys(stats?.categories || {}),
    datasets: [{
      data: Object.values(stats?.categories || {}).map((c: any) => c.count),
      backgroundColor: [
        '#FF6384', '#36A2EB', '#FFCE56', '#4BC0C0',
        '#9966FF', '#FF9F40', '#FF6384', '#C9CBCF'
      ]
    }]
  };
  
  return (
    <div>
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="Tổng Boss"
              value={stats?.overview.totalBosses}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Boss Hoạt Động"
              value={stats?.overview.activeBosses}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Sát Thương TB"
              value={stats?.stats.averageDame}
              formatter={(value) => value.toLocaleString()}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Máu TB"
              value={stats?.stats.averageHP}
              formatter={(value) => value.toLocaleString()}
            />
          </Card>
        </Col>
      </Row>
      
      <Row gutter={16}>
        <Col span={12}>
          <Card title="Phân bố theo Category">
            <Pie data={categoryData} />
          </Card>
        </Col>
        <Col span={12}>
          <Card title="Thống kê Dame">
            <Bar data={dameData} />
          </Card>
        </Col>
      </Row>
    </div>
  );
};
```

## Custom Hooks

### useBosses Hook
```typescript
import { useQuery } from 'react-query';
import { bossApi } from '../services/bossApi';

export const useBosses = (filters: BossFilters) => {
  return useQuery(
    ['bosses', filters],
    () => bossApi.getBosses(filters),
    {
      keepPreviousData: true,
      staleTime: 5 * 60 * 1000 // 5 minutes
    }
  );
};
```

### useBossForm Hook
```typescript
import { useState, useEffect } from 'react';
import { Form, message } from 'antd';
import { bossApi } from '../services/bossApi';

export const useBossForm = (bossId?: number) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  
  useEffect(() => {
    if (bossId) {
      loadBossData();
    }
  }, [bossId]);
  
  const loadBossData = async () => {
    try {
      const boss = await bossApi.getBoss(bossId!);
      form.setFieldsValue(boss);
    } catch (error) {
      message.error('Không thể tải dữ liệu boss');
    }
  };
  
  const onSubmit = async (values: BossFormData) => {
    setLoading(true);
    try {
      if (bossId) {
        await bossApi.updateBoss(bossId, values);
        message.success('Cập nhật boss thành công');
      } else {
        await bossApi.createBoss(values);
        message.success('Tạo boss thành công');
      }
    } catch (error) {
      message.error('Có lỗi xảy ra');
    } finally {
      setLoading(false);
    }
  };
  
  return { form, loading, onSubmit };
};
```

## API Service Layer

### bossApi.ts
```typescript
import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json'
  }
});

export const bossApi = {
  getBosses: (filters: BossFilters) => 
    api.get('/bosses', { params: filters }),
    
  getBoss: (id: number) => 
    api.get(`/bosses/${id}`),
    
  createBoss: (data: BossFormData) => 
    api.post('/bosses', data),
    
  updateBoss: (id: number, data: Partial<BossFormData>) => 
    api.put(`/bosses/${id}`, data),
    
  deleteBoss: (id: number) => 
    api.delete(`/bosses/${id}`),
    
  reloadBoss: (id: number) => 
    api.post(`/bosses/${id}/reload`),
    
  getStatistics: () => 
    api.get('/bosses/statistics'),
    
  getCategories: () => 
    api.get('/bosses/categories')
};
```

## WebSocket Integration

### useWebSocket Hook
```typescript
import { useEffect, useRef } from 'react';
import { message } from 'antd';

export const useWebSocket = (url: string) => {
  const wsRef = useRef<WebSocket | null>(null);
  
  useEffect(() => {
    wsRef.current = new WebSocket(url);
    
    wsRef.current.onmessage = (event) => {
      const data = JSON.parse(event.data);
      
      switch (data.type) {
        case 'BOSS_CREATED':
          message.success(`Boss mới: ${data.boss.name}`);
          break;
        case 'BOSS_UPDATED':
          message.info(`Boss đã cập nhật: ${data.boss.name}`);
          break;
        case 'BOSS_DELETED':
          message.warning(`Boss đã bị xóa: ID ${data.bossId}`);
          break;
      }
    };
    
    return () => {
      wsRef.current?.close();
    };
  }, [url]);
  
  return wsRef.current;
};
```

## Validation Schema

### bossValidation.ts
```typescript
import * as yup from 'yup';

export const bossSchema = yup.object({
  name: yup.string().required('Tên boss không được để trống'),
  gender: yup.number().min(0).max(2).required('Vui lòng chọn giới tính'),
  dame: yup.number().min(0).required('Sát thương phải lớn hơn 0'),
  hp: yup.array().of(yup.number().min(1)).min(1, 'Phải có ít nhất 1 HP'),
  mapJoin: yup.array().of(yup.number()).min(1, 'Phải chọn ít nhất 1 map'),
  secondsRest: yup.number().min(0),
  typeAppear: yup.number().min(0).max(2),
  isActive: yup.boolean(),
  outfits: yup.array().of(yup.object({
    slot: yup.string().required(),
    itemId: yup.number().required()
  })),
  skills: yup.array().of(yup.object({
    skillId: yup.number().required(),
    level: yup.number().min(1).max(7).required(),
    cooldown: yup.number().min(0).required()
  })),
  rewards: yup.array().of(yup.object({
    itemId: yup.number().required(),
    quantity: yup.number().min(1).required(),
    dropRate: yup.number().min(0).max(100).required()
  }))
});
```

## Deployment Guide

### 1. Frontend Build
```bash
npm run build
npm run preview
```

### 2. Backend Build
```bash
mvn clean package
java -jar target/boss-management-api.jar
```

### 3. Database Setup
```sql
-- Tạo database
CREATE DATABASE boss_management;

-- Import schema
mysql -u root -p boss_management < boss_database_schema.sql

-- Import sample data
mysql -u root -p boss_management < boss_sample_data.sql
```

### 4. Nginx Configuration
```nginx
server {
    listen 80;
    server_name boss-management.local;
    
    location / {
        root /var/www/boss-management/dist;
        try_files $uri $uri/ /index.html;
    }
    
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
    
    location /ws {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

---

**Hướng dẫn này cung cấp đầy đủ code examples và implementation details để xây dựng web interface quản lý boss system hoàn chỉnh.**
